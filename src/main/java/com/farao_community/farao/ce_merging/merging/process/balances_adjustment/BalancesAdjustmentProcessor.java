/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.AreasManager;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.BalancesPreprocessing;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.TargetNetPositionsImporter;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.ZonalDataImporter;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputation;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationArea;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationFactoryImpl;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult;
import com.powsybl.balances_adjustment.balance_computation.json_parameters.JsonBalanceComputationParameters;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.DefaultComputationManagerConfig;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.loadflow.LoadFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.AC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DC;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.LoadFlowUtils.runLoadflow;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_CORRECTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BalancesAdjustmentProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BalancesAdjustmentProcessor.class);
    private static final double MAXIMUM_NET_POSITION_MISMATCH_BEFORE_REDISPATCH = 1.;
    private static final double DEFAULT_PMAX = 9999.0;
    private static final double DEFAULT_PMIN = -9999.0;
    public static final String BUNDLE_BASE_NAME = "i18n.reports";
    public static final String BUNDLE_TEMPLATE = "balance.report";
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private final BalanceComputationParameters balanceComputationParameters;
    private final MergingTask task;
    private final Network network;
    private final Map<String, Double> targetNetPositions;
    private final Map<String, Scalable> countryToScalable;
    private Map<String, NetworkAreaFactory> networkAreas;
    private final List<BalanceComputationArea> areas;
    private final CeMergingConfiguration configuration;

    public BalancesAdjustmentProcessor(final MergingTask task,
                                       final CeMergingConfiguration configuration,
                                       final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                       final Supplier<BalanceComputationParameters> balanceComputationParametersSupplier) throws IOException {
        this.task = task;
        this.configuration = configuration;
        this.network = Network.read(Paths.get(task.getArtifactPath(TGM_FILE_AFTER_RECESSIVITY)));
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
        this.balanceComputationParameters = Optional.ofNullable(
                task.getConfigurations().getBalancesAdjustmentParameters().getPath()
            ).map(Paths::get)
            .map(JsonBalanceComputationParameters::read)
            .orElse(balanceComputationParametersSupplier.get());

        this.targetNetPositions = TargetNetPositionsImporter.getTargetNetPositionsAreasFromFile(
            Paths.get(task.getArtifactPath(BALANCES_ADJUSTMENT_TARGET_FILE)).toFile()
        );

        final Map<String, Scalable> eicScalable = ZonalDataImporter.getZonalDataFromGlsk(
            Paths.get(task.getArtifactPath(GLSK_QUALITY_CORRECTED_FILE)).toFile(), network, task.getTargetDate()
        );

        // For proportional scalable, if some scalable are saturated the scaling will be dispatched on the other
        balanceComputationParameters.getScalingParameters()
            .setPriority(ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED);

        final Map<String, String> countriesByEicCode = task.getConfigurations().getRegionConfiguration().getCountriesByEicCode();
        this.countryToScalable = eicScalable.entrySet()
            .stream()
            .collect(toMap(v -> countriesByEicCode.get(v.getKey()),
                           Map.Entry::getValue));

        this.areas = generateBalanceComputationAreas();

    }

    public void run() {
        try {
            BalancesPreprocessing.adjustGeneratorsPminWithTarget(network, areas);
            BalancesPreprocessing.integrateCompensation(network);
            balanceAreas();
        } catch (final Exception e) {
            String errorMessage = "Error in Balance computation process, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private Map<String, InitialBounds> setPminPmaxToDefaultValue() {
        Map<String, InitialBounds> initialBounds = new HashMap<>();
        AreasManager.on(areas, network).apply(generator -> {
            initialBounds.put(generator.getId(), new InitialBounds(generator.getMinP(), generator.getMaxP()));
            generator.setMinP(DEFAULT_PMIN);
            generator.setMaxP(DEFAULT_PMAX);
        });

        LOGGER.info("Pmax and Pmin are set to default values. Pmax = {}, Pmin = {}", DEFAULT_PMAX, DEFAULT_PMIN);
        return initialBounds;
    }

    private void resetInitialPminPmax(final Map<String, InitialBounds> initialBoundsMap) {
        AreasManager.on(areas, network).apply(generator -> {
            final InitialBounds initialBounds = initialBoundsMap.get(generator.getId());
            final double targetP = generator.getTargetP();
            generator.setMaxP(max(targetP, initialBounds.pMax()));
            generator.setMinP(min(targetP, initialBounds.pMin()));
        });
        LOGGER.info("Pmax and Pmin are reset to initial values.");
    }

    private List<BalanceComputationArea> generateBalanceComputationAreas() {
        runLoadflow(network, loadFlowRunnerSupplier, balanceComputationParameters.getLoadFlowParameters());

        this.networkAreas = network.getCountries()
            .stream()
            .collect(toMap(Country::toString, CountryAreaFactory::new));

        completeInputMaps();
        dispatchNetPositionsInconsistencies();

        return network
            .getCountries()
            .stream()
            .map(this::createBalanceComputationArea)
            .toList();
    }

    private void dispatchNetPositionsInconsistencies() {
        final double initialNetPositionSum = networkAreas.values().stream()
            .mapToDouble(networkArea -> networkArea.create(network).getNetPosition())
            .sum();
        final double targetNetPositionSum = networkAreas.keySet().stream()
            .mapToDouble(targetNetPositions::get)
            .sum();

        final double netPositionMismatch = initialNetPositionSum - targetNetPositionSum;

        if (abs(netPositionMismatch) < MAXIMUM_NET_POSITION_MISMATCH_BEFORE_REDISPATCH) {
            return;
        }

        LOGGER.warn("Important mismatch between initial total net positions ({}) and targeted ones ({}). a redispatch will occur on each area", initialNetPositionSum, targetNetPositionSum);

        final double absTargetNetPositionSum = networkAreas.keySet().stream()
            .mapToDouble(country -> abs(targetNetPositions.get(country)))
            .sum();

        networkAreas.forEach((country, areaFactory) -> {
            double initialTarget = targetNetPositions.get(country);
            double finalTarget = initialTarget + netPositionMismatch * abs(initialTarget) / absTargetNetPositionSum;
            LOGGER.info("Country {} target net position modified from {} to {}", country, initialTarget, finalTarget);
            targetNetPositions.put(country, finalTarget);
        });
    }

    private void completeInputMaps() {
        network.getCountries().forEach(country -> {
            final String countryCode = country.toString();
            final NetworkAreaFactory countryArea = networkAreas.get(countryCode);
            targetNetPositions.computeIfAbsent(countryCode, s -> countryArea.create(network).getNetPosition());
            countryToScalable.computeIfAbsent(countryCode, s -> createCountryLsk(country));
        });
    }

    private BalanceComputationArea createBalanceComputationArea(final Country country) {
        final String countryCode = country.toString();
        final NetworkAreaFactory networkArea = networkAreas.get(countryCode);
        final Scalable scalable = countryToScalable.get(countryCode);
        final double targetNetPosition = targetNetPositions.get(countryCode);
        return new BalanceComputationArea(country.getName(), networkArea, scalable, targetNetPosition);
    }

    private Scalable createCountryLsk(final Country country) {
        LOGGER.info("No available shift for {}. Balance adjustment is performed proportionally on all loads.", country);

        final List<Load> countryLoads = network.getLoadStream()
            .filter(isSynchronizedWithCountry(country))
            .toList();
        // In case of loads with negative flow (meaning power generation), we should take absolute value.
        // If that's the case, if the shift for country is positive,
        // loads with negative power will decrease (generation increase) and so on
        final double totalCountryP = countryLoads.stream()
            .map(Load::getP0)
            .mapToDouble(Math::abs)
            .sum();

        final List<Scalable> scalables = new ArrayList<>();
        final List<Double> percentages = new ArrayList<>();
        countryLoads.forEach(load -> {
            final double percentage = 100 * abs(load.getP0()) / totalCountryP;
            percentages.add(percentage);
            scalables.add(Scalable.onLoad(load.getId(), -MAX_VALUE, MAX_VALUE));
        });

        return Scalable.proportional(percentages, scalables);

    }

    private static Predicate<Load> isSynchronizedWithCountry(final Country country) {
        return load -> load.getTerminal().isConnected()
                       && hasBusInMainSynchronousComponent(load)
                       && isLoadInCountry(load, country);
    }

    private static boolean hasBusInMainSynchronousComponent(final Load load) {
        final Bus bus = load.getTerminal().getBusBreakerView().getConnectableBus();
        return bus != null && bus.isInMainSynchronousComponent();
    }

    private static boolean isLoadInCountry(final Load load, final Country country) {
        return load.getTerminal()
            .getVoltageLevel()
            .getSubstation()
            .map(Substation::getNullableCountry)
            .map(country::equals)
            .orElse(false);
    }

    private void balanceAreas() throws IOException {
        final Map<String, InitialBounds> initGenerators = setPminPmaxToDefaultValue();

        final BalanceComputation balanceComputation = new BalanceComputationFactoryImpl()
            .create(areas,
                    loadFlowRunnerSupplier.get(),
                    DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());

        final String loadflowMode = balanceComputationParameters.getLoadFlowParameters().isDc() ? DC : AC;

        final ReportNode rootReportNode = ReportNode.newRootReportNode()
            .withResourceBundles(BUNDLE_BASE_NAME)
            .withMessageTemplate(BUNDLE_TEMPLATE)
            .build();

        BalanceComputationResult result = balanceComputation
            .run(network, getVariantId(), balanceComputationParameters, rootReportNode)
            .join();

        if (isSuccessful(result)) {
            exportOutputs(initGenerators, loadflowMode, result);
        } else { // DC Fallback
            final String detailMessage = getFailureMessage(loadflowMode, result);
            LOGGER.warn(detailMessage);

            if (DC.equals(loadflowMode)) {
                throw new CeMergingException(detailMessage);
            }

            LOGGER.info("Trying a second balances adjustment with DC Loadflow");
            balanceComputationParameters.getLoadFlowParameters().setDc(true);
            result = balanceComputation.run(network, getVariantId(), balanceComputationParameters).join();

            if (isSuccessful(result)) {
                exportOutputs(initGenerators, DC, result);
            } else {
                handleComputationFailure(result, rootReportNode);
            }
        }
    }

    private boolean isSuccessful(final BalanceComputationResult result) {
        return result.getStatus() == BalanceComputationResult.Status.SUCCESS;
    }

    private String getVariantId() {
        return network.getVariantManager().getWorkingVariantId();
    }

    private void handleComputationFailure(final BalanceComputationResult result,
                                          final ReportNode reportNode) {
        int maxNumberIteration = balanceComputationParameters.getMaxNumberIterations();
        int failedIterationNumber = result.getIterationCount();

        if (failedIterationNumber >= maxNumberIteration) {
            final BalancesAdjustmentSummary balancesAdjustmentSummary = new BalancesAdjustmentSummary(network, reportNode, maxNumberIteration);
            balancesAdjustmentSummary.print();
        }

        String failureMessage = getFailureMessage(DC, result);
        LOGGER.error(failureMessage);

        throw new CeMergingException(failureMessage);
    }

    private String getFailureMessage(final String loadflowMode,
                                     final BalanceComputationResult result) {
        return "Balances adjustment failed with "
               + loadflowMode
               + " Loadflow because "
               + getFailureReason(result);
    }

    private String getFailureReason(final BalanceComputationResult result) {
        final int failedIterationNumber = result.getIterationCount();
        final int maxIterations = balanceComputationParameters.getMaxNumberIterations();

        if (failedIterationNumber >= maxIterations) {
            return "the maximum number of iterations (%s) was reached, but mismatch is still above threshold (%s)"
                .formatted(maxIterations, balanceComputationParameters.getThresholdNetPosition());
        } else {
            return "the load flow diverged at iteration n°%s ; maybe du to a shift demand much higher than initial net positions"
                .formatted(failedIterationNumber);
        }
    }

    void exportOutputs(final Map<String, InitialBounds> initialBounds,
                       final String loadflowMode,
                       final BalanceComputationResult result) {

        LOGGER.info("BalanceComputation status is finished with success with {} Loadflow, iterations number is {}.",
                    loadflowMode, result.getIterationCount());

        if (DC.equals(loadflowMode)) {
            LOGGER.info("Changing loadflow parameters to DC mode for task {}", task.getId());
        }

        final File cgmFileAdjusted = createAdjustedCgm(initialBounds,
                                                       task.getArtifacts()
                                                                      .getFile(BALANCES_ADJUSTMENT_TARGET_FILE)
                                                                      .getOriginalName());

        task.setArtifact(BALANCED_CGM_FILE, new SavedFile(cgmFileAdjusted.getName(),
                                                          cgmFileAdjusted.getPath(),
                                                          String.format("/tasks/%d/outputs/shifted-cgm",
                                                                        task.getId())));

    }

    private File createAdjustedCgm(Map<String, InitialBounds> initialBounds,
                                   String cgmFileName) {
        final File cgmFileAdjusted = new File(configuration.getArtifactsDirectoryPath(task)
                                              + File.separator
                                              + cgmFileName
                                              + "_adjusted.xiidm");

        resetInitialPminPmax(initialBounds);
        network.write("XIIDM", null, Paths.get(cgmFileAdjusted.getPath()));
        return cgmFileAdjusted;
    }


    private record InitialBounds(double pMin, double pMax) {
    }
}

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
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
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
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.AC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DC;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.LoadFlowUtils.runLoadflow;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.TargetNetPositionsImporter.getTargetNetPositionsAreasFromFile;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.ZonalDataImporter.getZonalDataFromGlsk;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_CORRECTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult.Status.SUCCESS;
import static com.powsybl.iidm.modification.scalable.ScalingParameters.Priority.RESPECT_OF_VOLUME_ASKED;
import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.isNaN;
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
    private final Map<String, Scalable> loadShiftKeysByCountry;
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
        final String balanceConfigPath = task.getConfigurations().getBalancesAdjustmentParameters().getPath();
        this.balanceComputationParameters = Optional.ofNullable(balanceConfigPath)
            .map(Paths::get)
            .map(JsonBalanceComputationParameters::read)
            .orElse(balanceComputationParametersSupplier.get());

        this.targetNetPositions = getTargetNetPositionsAreasFromFile(task.getArtifactFile(BALANCES_ADJUSTMENT_TARGET_FILE));
        final Map<String, Scalable> eicScalable = getZonalDataFromGlsk(task.getArtifactFile(GLSK_QUALITY_CORRECTED_FILE),
                                                                       network, task.getTargetDate());

        // For proportional scalable, if some scalable are saturated the scaling will be dispatched on the other
        balanceComputationParameters.getScalingParameters().setPriority(RESPECT_OF_VOLUME_ASKED);

        this.loadShiftKeysByCountry = eicScalable.entrySet()
            .stream()
            .collect(toMap(v -> task.getConfigurations().getRegionConfiguration().getCountriesByEicCode().get(v.getKey()),
                           Map.Entry::getValue));

        this.areas = generateAreas();

    }

    public void run() {
        try {
            AreasManager.on(areas, network).applyToGenerators(BalancesAdjustmentProcessor::updateToTrueMinP);
            network.getGeneratorStream().forEach(BalancesAdjustmentProcessor::compensate);
            balanceAreas();
        } catch (final Exception e) {
            String errorMessage = "Error in Balance computation process, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private static void updateToTrueMinP(final Generator generator) {
        generator.setMinP(min(generator.getMinP(), generator.getTargetP()));
    }

    private static void compensate(final Generator generator) {
        final double terminalPower = generator.getTerminal().getP();
        if (!isNaN(terminalPower)) {
            generator.setTargetP(-terminalPower);
        }
    }

    private Map<String, InitialBounds> swapWithDefaultMinMaxP() {
        Map<String, InitialBounds> initialBounds = new HashMap<>();
        AreasManager.on(areas, network).applyToGenerators(generator -> {
            initialBounds.put(generator.getId(), new InitialBounds(generator.getMinP(), generator.getMaxP()));
            generator.setMinP(DEFAULT_PMIN);
            generator.setMaxP(DEFAULT_PMAX);
        });

        LOGGER.info("Pmax and Pmin are set to default values. Pmax = {}, Pmin = {}", DEFAULT_PMAX, DEFAULT_PMIN);
        return initialBounds;
    }

    private void swapBackValidInitialMinMaxP(final Map<String, InitialBounds> initialBoundsMap) {
        AreasManager.on(areas, network).applyToGenerators(generator -> {
            final InitialBounds initialBounds = initialBoundsMap.get(generator.getId());
            final double targetP = generator.getTargetP();
            generator.setMaxP(max(targetP, initialBounds.pMax()));
            generator.setMinP(min(targetP, initialBounds.pMin()));
        });
        LOGGER.info("Pmax and Pmin are reset to initial values.");
    }

    private List<BalanceComputationArea> generateAreas() {
        runLoadflow(network, loadFlowRunnerSupplier, balanceComputationParameters.getLoadFlowParameters());

        this.networkAreas = network.getCountries().stream().collect(toMap(Country::toString, CountryAreaFactory::new));

        createMissingData();
        distributeNetPositionsMismatch();

        return network
            .getCountries()
            .stream()
            .map(this::createArea)
            .toList();
    }

    private void distributeNetPositionsMismatch() {
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
            .map(targetNetPositions::get)
            .mapToDouble(Math::abs)
            .sum();

        networkAreas.forEach((country, areaFactory) -> {
            double initialTarget = targetNetPositions.get(country);
            double finalTarget = initialTarget + netPositionMismatch * abs(initialTarget) / absTargetNetPositionSum;
            LOGGER.info("Country {} target net position modified from {} to {}", country, initialTarget, finalTarget);
            targetNetPositions.put(country, finalTarget);
        });
    }

    private void createMissingData() {
        network.getCountries().forEach(country -> {
            final String countryCode = country.toString();
            final NetworkAreaFactory countryArea = networkAreas.get(countryCode);
            targetNetPositions.computeIfAbsent(countryCode, s -> countryArea.create(network).getNetPosition());
            loadShiftKeysByCountry.computeIfAbsent(countryCode, s -> createLoadShiftKey(country));
        });
    }

    private BalanceComputationArea createArea(final Country country) {
        final String countryCode = country.toString();
        final NetworkAreaFactory networkArea = networkAreas.get(countryCode);
        final Scalable lsk = loadShiftKeysByCountry.get(countryCode);
        final double targetNetPosition = targetNetPositions.get(countryCode);
        return new BalanceComputationArea(country.getName(), networkArea, lsk, targetNetPosition);
    }

    private Scalable createLoadShiftKey(final Country country) {
        LOGGER.info("No available shift for {}. Balance adjustment is performed proportionally on all loads.", country);

        final List<Load> countryLoads = network.getLoadStream()
            .filter(load -> isSynchronizedWithCountry(load.getTerminal(), country))
            .toList();
        // In case of loads with negative flow (meaning power generation), we should take absolute value.
        // meaning if a country's shift is positive,  loads with negative power will decrease (generation increase) and inversely
        final double totalCountryP = countryLoads.stream()
            .map(Load::getP0)
            .mapToDouble(Math::abs)
            .sum();

        final List<Scalable> lsks = new ArrayList<>();
        final List<Double> percentages = new ArrayList<>();
        countryLoads.forEach(load -> {
            final double percentage = 100 * abs(load.getP0()) / totalCountryP;
            percentages.add(percentage);
            lsks.add(Scalable.onLoad(load.getId(), -MAX_VALUE, MAX_VALUE));
        });

        return Scalable.proportional(percentages, lsks);

    }

    private static boolean isSynchronizedWithCountry(final Terminal terminal, final Country country) {
        return terminal.isConnected() && hasBusInMainSynchronousComponent(terminal) && isInCountry(terminal, country);
    }

    private static boolean hasBusInMainSynchronousComponent(final Terminal terminal) {
        final Bus bus = terminal.getBusBreakerView().getConnectableBus();
        return bus != null && bus.isInMainSynchronousComponent();
    }

    private static boolean isInCountry(final Terminal terminal, final Country country) {
        return terminal.getVoltageLevel()
            .getSubstation()
            .map(Substation::getNullableCountry)
            .map(country::equals)
            .orElse(false);
    }

    private void balanceAreas() {
        final Map<String, InitialBounds> initGenerators = swapWithDefaultMinMaxP();

        final BalanceComputation balanceComputation = new BalanceComputationFactoryImpl()
            .create(areas, loadFlowRunnerSupplier.get(), DefaultComputationManagerConfig.load().createShortTimeExecutionComputationManager());

        final String loadflowMode = balanceComputationParameters.getLoadFlowParameters().isDc() ? DC : AC;

        final ReportNode rootReportNode = ReportNode.newRootReportNode()
            .withResourceBundles(BUNDLE_BASE_NAME)
            .withMessageTemplate(BUNDLE_TEMPLATE)
            .build();

        BalanceComputationResult result = balanceComputation
            .run(network, getVariantId(), balanceComputationParameters, rootReportNode)
            .join();

        if (result.getStatus() == SUCCESS) {
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

            if (result.getStatus() == SUCCESS) {
                exportOutputs(initGenerators, DC, result);
            } else {
                handleComputationFailure(result, rootReportNode);
            }
        }
    }

    private String getVariantId() {
        return network.getVariantManager().getWorkingVariantId();
    }

    private void handleComputationFailure(final BalanceComputationResult result,
                                          final ReportNode reportNode) {
        int maxNumberIteration = balanceComputationParameters.getMaxNumberIterations();
        int failedIteration = result.getIterationCount();

        if (failedIteration >= maxNumberIteration) {
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
            return "the load flow diverged at iteration n°%s ; maybe due to a shift demand much higher than initial net positions"
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

    private File createAdjustedCgm(final Map<String, InitialBounds> initialBounds,
                                   final String cgmFileName) {
        final File cgmFileAdjusted = new File(configuration.getArtifactsDirectoryPath(task)
                                              + File.separator
                                              + cgmFileName
                                              + "_adjusted.xiidm");

        swapBackValidInitialMinMaxP(initialBounds);
        network.write("XIIDM", null, Paths.get(cgmFileAdjusted.getPath()));
        return cgmFileAdjusted;
    }


    private record InitialBounds(double pMin, double pMax) {
    }
}

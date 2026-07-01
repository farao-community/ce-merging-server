/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.Interval;
import com.farao_community.farao.ce_merging.base_case_improvement.forecast_netpositions.ReferenceProgram;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.AlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.AlegroFlows;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.BciAlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.BciAlegroFlows;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciComputationResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciProcessResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.JsonBciResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.OutRegionResults;
import com.farao_community.farao.ce_merging.base_case_improvement.task.BciOutput;
import com.farao_community.farao.ce_merging.base_case_improvement.task.BciTask;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.ExternalConstraintsImporter.calculateConstraintsForAlegro;
import static com.farao_community.farao.ce_merging.base_case_improvement.initial_netpositions.InitialNetPositionsImporter.getGlobalNetPosition;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static java.util.Collections.emptyMap;

public class BciProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(BciProcess.class);
    private static final String BCI_OUTPUT_FILE_NAME = "bciOutput.json";
    private final BciTask task;
    private final RegionConfiguration regionConfiguration;
    private final CeMergingConfiguration configuration;
    private BciProcessResult processResult;
    private final Map<String, Double> initialRegionNetPositions = new HashMap<>();
    private Map<String, Double> initialGlobalNetPositions;
    private Map<String, Interval> regionFeasibilityRanges;
    private ReferenceProgram referenceProgram;
    private BciOutput bciOutput;

    public BciProcess(final BciTask task,
                      final CeMergingConfiguration configuration,
                      final RegionConfiguration regionConfiguration) {
        this.task = task;
        this.configuration = configuration;
        this.regionConfiguration = regionConfiguration;
    }

    public void run() {
        importFiles();
        computeBci();
        createBciOutput();
        task.setBciOutput(bciOutput);
    }

    private String getAlegroNetPosPath() {
        return task.getBciInputs().getAlegroNetPositionsPath();
    }

    private String getExternalConstraintsPath() {
        return task.getBciInputs().getAlegroNetPositionsPath();
    }

    private void importFiles() {
        importNpf();
        importInitialGlobalNetPositions();
        computeInRegionNetPositions();
        Optional.ofNullable(getAlegroNetPosPath()).ifPresent(this::updateInRegionNetPositions);
        importRegionFeasibilityRanges();
    }

    private void computeInRegionNetPositions() {
        Map<String, Double> outRegionNetPositions = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);
        initialGlobalNetPositions.forEach((region, inNp) -> {
            final double outNp = outRegionNetPositions.getOrDefault(region, 0.);
            initialRegionNetPositions.put(region, inNp - outNp);
        });

    }

    private void updateInRegionNetPositions(final String alegroNetPositionsPath) {
        final AlegroData alegroData = JsonUtils.read(AlegroData.class, alegroNetPositionsPath);

        final double alDeDisplayValue = getAlegroConstrainedTargetFlow(alegroData, alegroData.getAlDeFlows());
        final double germanAlegroGap = alDeDisplayValue - alegroData.getAlDeFlows().getInitialFlow();
        final double alBeDisplayValue = getAlegroConstrainedTargetFlow(alegroData, alegroData.getAlBeFlows());
        final double belgianAlegroGap = alBeDisplayValue - alegroData.getAlBeFlows().getInitialFlow();

        final String germany = regionConfiguration.getAreasIn().get("DE");
        final String belgium = regionConfiguration.getAreasIn().get("BE");

        initialRegionNetPositions.computeIfPresent(germany, (k, np) -> np + germanAlegroGap);
        initialRegionNetPositions.computeIfPresent(belgium, (k, np) -> np + belgianAlegroGap);
    }

    private Map<String, Interval> getAlegroExternalConstraints() {
        try {
            final byte[] externalConstraints = Files.readAllBytes(Paths.get(getExternalConstraintsPath()));
            return calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
        } catch (final IOException e) {
            String errorMessage = "Could not import external constraints file, " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }

    }

    private double getAlegroConstrainedTargetFlow(final AlegroData alegroData,
                                                  final AlegroFlows toConstrain) {
        final double maxFlow = getCommonLimit(alegroData, getAlegroExternalConstraints());
        return alegroData.getAlegroInOutage() ? 0 : Math.clamp(toConstrain.getTargetFlow(), -maxFlow, maxFlow);
    }

    private void importNpf() {
        try {
            LOGGER.info("Importing forecast net positions file: " + FilenameUtils.getName(task.getBciInputs().getForecastNetPositionsPath()));
            referenceProgram = JsonUtils.read(ReferenceProgram.class, task.getBciInputs().getForecastNetPositionsPath());
        } catch (final Exception e) {
            String errorMessage = "Could not import forecast net positions file, " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void computeBci() {
        final BciComputation computation = new BciComputation(regionConfiguration, referenceProgram);
        boolean isMergingWithAlegro = getAlegroNetPosPath() != null;

        final double alDeDisplayValue;
        final double alBeDisplayValue;
        if (isMergingWithAlegro) {
            final AlegroData alegroData = JsonUtils.read(AlegroData.class, getAlegroNetPosPath());
            alDeDisplayValue = getAlegroConstrainedTargetFlow(alegroData, alegroData.getAlDeFlows());
            alBeDisplayValue = getAlegroConstrainedTargetFlow(alegroData, alegroData.getAlBeFlows());
        }
        else {
            alDeDisplayValue = 0;
            alBeDisplayValue = 0;
        }

        final BciComputationResult bciResults = computation.run(regionFeasibilityRanges,
                                                                initialRegionNetPositions,
                                                                alBeDisplayValue,
                                                                alDeDisplayValue);

        processResult = new BciProcessResult(regionConfiguration.getName(),
                                             task.getProcessTargetDate(),
                                             bciResults,
                                             getOutRegionResults(),
                                             isMergingWithAlegro ? getBciAlegroData() : null);
    }

    private BciAlegroData getBciAlegroData() {
        final AlegroData alegroData = JsonUtils.read(AlegroData.class, getAlegroNetPosPath());

        if (alegroData.getAlegroInOutage()) {
            return null;
        }

        final byte[] externalConstraints;
        try {
            externalConstraints = Files.readAllBytes(Paths.get(getExternalConstraintsPath()));
        } catch (final IOException e) {
            String errorMessage = "Cannot import external constraint fie, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }

        final Map<String, Interval> alegroEc = calculateConstraintsForAlegro(externalConstraints,
                                                                             task.getProcessTargetDate());
        final Interval alDeConstraints = alegroEc.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroEc.get(ALEGRO_BE_NODE_NAME);

        final BciAlegroFlows alDeFlows = new BciAlegroFlows(alegroData.getAlDeFlows().getTargetFlow(),
                                                            alDeConstraints.getMinValue(),
                                                            alDeConstraints.getMaxValue());

        final BciAlegroFlows alBeFlows = new BciAlegroFlows(alegroData.getAlBeFlows().getTargetFlow(),
                                                            alBeConstraints.getMinValue(),
                                                            alBeConstraints.getMaxValue());

        return new BciAlegroData(alDeFlows, alBeFlows);
    }


    private OutRegionResults getOutRegionResults() {
        final Map<String, Double> globalNetPositionsByAreaId = referenceProgram
            .computeGlobalNetPositionsForOutAreas(regionConfiguration);

        final Map<String, Double> globalNetPositionsByCountry = regionConfiguration
            .getAreasOut()
            .entrySet()
            .stream()
            .collect(sameKeysValuesFrom(globalNetPositionsByAreaId));

        return new OutRegionResults(globalNetPositionsByCountry);
    }

    private Collector<Map.Entry<String,String>, ?, TreeMap<String, Double>> sameKeysValuesFrom(
        final Map<String, Double> globalNetPositionsByAreaId
    ) {
        return Collectors.toMap(Map.Entry::getKey,
                                extractedFrom(globalNetPositionsByAreaId),
                                (o1, o2) -> o1,
                                TreeMap::new);
    }

    private Function<Map.Entry<String, String>, Double> extractedFrom(final Map<String, Double> globalNetPositionsByAreaId) {
        return e -> globalNetPositionsByAreaId.getOrDefault(e.getValue(), 0.);
    }

    private void createBciOutput() {
        final String outputPath = configuration.getOutputsDirectoryPath(task) + File.separator + BCI_OUTPUT_FILE_NAME;
        try (final OutputStream outputStream = new FileOutputStream(outputPath)) {
            JsonBciResult.write(processResult, outputStream);
            bciOutput = new BciOutput(outputPath);
        } catch (final Exception e) {
            String errorMessage = "Cannot create base case improvement output fie, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void importInitialGlobalNetPositions() {
        initialGlobalNetPositions = Optional.ofNullable(task.getBciInputs().getInitialNetPositionsPath())
            .map(this::safeImportGlobalNp)
            .orElse(emptyMap());
    }

    private Map<String, Double> safeImportGlobalNp(final String path) {
        try {
            return getGlobalNetPosition(new FileInputStream(path), regionConfiguration);
        } catch (final Exception e) {
            final String errorMessage = "Error during import of initial net positions"
                                        + FilenameUtils.getName(path)
                                        + ", cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void importRegionFeasibilityRanges() {
        try {
            final byte[] externalConstraints = Files.readAllBytes(Paths.get(getExternalConstraintsPath()));
            final FeasibilityRangeCalculator calculator = new FeasibilityRangeCalculator(regionConfiguration);
            final String feasibilityRangePath = task.getBciInputs().getFeasibilityRangePath();

            final byte[] feasibilityRange = feasibilityRangePath != null ?
                Files.readAllBytes(Paths.get(feasibilityRangePath)) : new byte[0];

            regionFeasibilityRanges = calculator.getRegionFeasibilityRanges(externalConstraints,
                                                                            task.getProcessTargetDate(),
                                                                            initialRegionNetPositions,
                                                                            feasibilityRange);
        } catch (final Exception e) {
            String errorMessage = "Could not calculate feasibility ranges, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private double getCommonLimit(final AlegroData bciAlegroData,
                                  final Map<String, Interval> alegroExternalConstraints) {

        final Interval alDeConstraints = alegroExternalConstraints.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroExternalConstraints.get(ALEGRO_BE_NODE_NAME);

        final double minEcAlDe = alDeConstraints.getMinValue();
        final double maxEcAlDe = alDeConstraints.getMaxValue();
        final double minEcAlBe = alBeConstraints.getMinValue();
        final double maxEcAlBe = alBeConstraints.getMaxValue();

        final boolean flowsToGermany = bciAlegroData.getAlBeFlows().getTargetFlow() < 0;

        final double maxAtOrigin = Math.abs(flowsToGermany ? maxEcAlBe : maxEcAlDe);
        // because min < 0
        final double maxAtDestination = Math.abs(flowsToGermany ? minEcAlDe : minEcAlBe);

        return Math.min(maxAtOrigin, maxAtDestination);
    }
}

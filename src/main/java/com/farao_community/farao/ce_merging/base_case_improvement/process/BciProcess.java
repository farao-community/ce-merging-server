/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.AlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.BciAlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.process.data.BciAlegroFlows;
import com.farao_community.farao.ce_merging.base_case_improvement.task.BciOutput;
import com.farao_community.farao.ce_merging.base_case_improvement.task.BciTask;
import com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.ExternalConstraintsImporter;
import com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.Interval;
import com.farao_community.farao.ce_merging.base_case_improvement.forecast_netpositions.ReferenceProgram;
import com.farao_community.farao.ce_merging.base_case_improvement.initial_netpositions.InitialNetPositionsImporter;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciComputationResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciProcessResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.JsonBciResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.OutRegionResults;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;

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
        importRequiredData();
        computeBci();
        createBciOutput();
        updateOutputTask();
    }

    private void importRequiredData() {
        importForecastNetPositionFile();
        computeInRegionNetPositions();
        if (task.getBciInputs().getAlegroNetPositionsPath() != null) {
            updateInRegionNetPositions();
        }
        importRegionFeasibilityRanges();
    }

    private void computeInRegionNetPositions() {
        importInitialGlobalNetPositions();
        Map<String, Double> outRegionNetPositions = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);
        initialGlobalNetPositions.forEach((k, v) -> initialRegionNetPositions.put(k, v - outRegionNetPositions.getOrDefault(k, 0.)));
    }

    private void updateInRegionNetPositions() {
        AlegroData alegroData = JsonUtils.read(AlegroData.class, task.getBciInputs().getAlegroNetPositionsPath());
        Map<String, Interval> externalContraintsMapForAlegro = getExternalConstraintsForAlegro();
        double maxEc = getCommonLimit(alegroData, externalContraintsMapForAlegro);
        double minEc = -maxEc;
        double valueAldeThatShouldBeDisplayedInCgm = getFinalFlowToBeDisplayedInCgm(minEc, maxEc, alegroData, alegroData.getAldeFlows().getTargetFlow());
        double gapBetweenFinalFlowAndInitialFlowForALde = valueAldeThatShouldBeDisplayedInCgm - alegroData.getAldeFlows().getInitialFlow();
        double valueAlbeThatShouldBeDisplayedInCgm = getFinalFlowToBeDisplayedInCgm(minEc, maxEc, alegroData, alegroData.getAlbeFlows().getTargetFlow());
        double gapBetweenFinalFlowAndInitialFlowForAlbe = valueAlbeThatShouldBeDisplayedInCgm - alegroData.getAlbeFlows().getInitialFlow();
        initialRegionNetPositions.put(regionConfiguration.getAreasIn().get("BE"), initialRegionNetPositions.get(regionConfiguration.getAreasIn().get("BE")) + gapBetweenFinalFlowAndInitialFlowForAlbe);
        initialRegionNetPositions.put(regionConfiguration.getAreasIn().get("DE"), initialRegionNetPositions.get(regionConfiguration.getAreasIn().get("DE")) + gapBetweenFinalFlowAndInitialFlowForALde);
    }

    private Map<String, Interval> getExternalConstraintsForAlegro() {
        try {
            byte[] externalConstraints = Files.readAllBytes(Paths.get(task.getBciInputs().getExternalConstraintsPath()));
            return ExternalConstraintsImporter.calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
        } catch (IOException e) {
            String errorMessage = "Couldn't import external constraints file, " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }

    }

    private double getFinalFlowToBeDisplayedInCgm(final double minEc,
                                                  final double maxEc,
                                                  final AlegroData alegroData,
                                                  final double targetFlow) {
        double valueTobeAdjustedTo = 0;
        if (Boolean.FALSE.equals(alegroData.getAlegroInOutage())) {
            if (targetFlow > maxEc) {
                valueTobeAdjustedTo = maxEc;
            } else if (targetFlow < minEc) {
                valueTobeAdjustedTo = minEc;
            } else {
                valueTobeAdjustedTo = targetFlow;
            }
        }
        return valueTobeAdjustedTo;
    }

    private void updateOutputTask() {
        task.setBciOutput(bciOutput);
    }

    private void importForecastNetPositionFile() {
        try {
            LOGGER.info("Importing forecast net positions file: " + FilenameUtils.getName(task.getBciInputs().getForecastNetPositionsPath()));
            referenceProgram = JsonUtils.read(ReferenceProgram.class, task.getBciInputs().getForecastNetPositionsPath());
        } catch (Exception e) {
            String errorMessage = "Couldn't import forecast net positions file, " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void computeBci() {
        double valueAldeThatShouldBeDisplayedInCgm = 0;
        double valueAlbeThatShouldBeDisplayedInCgm = 0;
        BciComputation bciComputation = new BciComputation(regionConfiguration, referenceProgram);
        boolean isMergingWithAlegro = task.getBciInputs().getAlegroNetPositionsPath() != null;
        if (isMergingWithAlegro) {
            AlegroData alegroData = JsonUtils.read(AlegroData.class, task.getBciInputs().getAlegroNetPositionsPath());
            Map<String, Interval> externalContraintsMapForAlegro = getExternalConstraintsForAlegro();
            double macEc = getCommonLimit(alegroData, externalContraintsMapForAlegro);
            double minEc = -macEc;
            valueAldeThatShouldBeDisplayedInCgm = getFinalFlowToBeDisplayedInCgm(minEc, macEc, alegroData, alegroData.getAldeFlows().getTargetFlow());
            valueAlbeThatShouldBeDisplayedInCgm = getFinalFlowToBeDisplayedInCgm(minEc, macEc, alegroData, alegroData.getAlbeFlows().getTargetFlow());
        }
        BciComputationResult bciResults = bciComputation.run(regionFeasibilityRanges, initialRegionNetPositions, valueAlbeThatShouldBeDisplayedInCgm, valueAldeThatShouldBeDisplayedInCgm);
        OutRegionResults outRegionResults = getOutRegionResults();
        if (task.getBciInputs().getAlegroNetPositionsPath() != null) {
            BciAlegroData bciAlegroData = getBciAlegroData();
            processResult = new BciProcessResult(regionConfiguration.getName(), task.getProcessTargetDate(), bciResults, outRegionResults, bciAlegroData);
        } else {
            processResult = new BciProcessResult(regionConfiguration.getName(), task.getProcessTargetDate(), bciResults, outRegionResults, null);
        }
    }

    private BciAlegroData getBciAlegroData() {
        AlegroData alegroData = JsonUtils.read(AlegroData.class, task.getBciInputs().getAlegroNetPositionsPath());
        if (!alegroData.getAlegroInOutage()) {
            byte[] externalConstraints;
            try {
                externalConstraints = Files.readAllBytes(Paths.get(task.getBciInputs().getExternalConstraintsPath()));
            } catch (IOException e) {
                String errorMessage = "Cannot import external constraint fie, cause: " + e.getMessage();
                LOGGER.error(errorMessage);
                throw new CeMergingException(errorMessage);
            }

            Map<String, Interval> externalContraintsMapForAlegro = ExternalConstraintsImporter.calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
            BciAlegroFlows aldeFlows = new BciAlegroFlows(alegroData.getAldeFlows().getTargetFlow(), externalContraintsMapForAlegro.get(ALEGRO_DE_NODE_NAME).getMinValue(),
                                                          externalContraintsMapForAlegro.get(ALEGRO_DE_NODE_NAME).getMaxValue());

            BciAlegroFlows albeFlows = new BciAlegroFlows(alegroData.getAlbeFlows().getTargetFlow(), externalContraintsMapForAlegro.get(ALEGRO_BE_NODE_NAME).getMinValue(),
                    externalContraintsMapForAlegro.get(ALEGRO_BE_NODE_NAME).getMaxValue());
            return new BciAlegroData(aldeFlows, albeFlows);
        } else {
            return null;
        }
    }

    private OutRegionResults getOutRegionResults() {
        Map<String, Double> globalNetPositionsByAreaId = referenceProgram.computeGlobalNetPositionsForOutAreas(regionConfiguration);
        Map<String, Double> globalNetPositionsByCountry = regionConfiguration.getAreasOut().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> globalNetPositionsByAreaId.getOrDefault(entry.getValue(), 0.), (o1, o2) -> o1, TreeMap::new));
        return new OutRegionResults(globalNetPositionsByCountry);
    }

    private void createBciOutput() {
        String outputPath = configuration.getOutputsDirectoryPath(task) + File.separator + BCI_OUTPUT_FILE_NAME;
        try (OutputStream outputStream = new FileOutputStream(outputPath)) {
            JsonBciResult.write(processResult, outputStream);
            bciOutput = new BciOutput(outputPath);
        } catch (Exception e) {
            String errorMessage = "Cannot create base case improvement output fie, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void importInitialGlobalNetPositions() {
        initialGlobalNetPositions = Optional.ofNullable(task.getBciInputs()
                .getInitialNetPositionsPath())
                .map(this::safeImportFile)
                .orElse(Collections.emptyMap());
    }

    private Map<String, Double> safeImportFile(final String path) {
        try {
            return InitialNetPositionsImporter.getGlobalNetPosition(new FileInputStream(path), regionConfiguration);
        } catch (Exception e) {
            String errorMessage = "Error during import of initial net positions" + FilenameUtils.getName(path) + ", cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void importRegionFeasibilityRanges() {
        try {
            byte[] externalConstraints;
            try {
                externalConstraints = Files.readAllBytes(Paths.get(task.getBciInputs().getExternalConstraintsPath()));
            } catch (IOException e) {
                String errorMessage = "Error during import of external constraints file, cause: " + e.getMessage();
                LOGGER.error(errorMessage);
                throw new CeMergingException(errorMessage);
            }

            FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
            byte[] feasibilityRange = new byte[0];
            Optional<String> feasibilityRangePath = Optional.ofNullable(task.getBciInputs().getFeasibilityRangePath());
            if (feasibilityRangePath.isPresent()) {
                LOGGER.info("Feasibility range file is present and will be imported");
                try {
                    feasibilityRange = Files.readAllBytes(Paths.get(feasibilityRangePath.get()));
                } catch (IOException e) {
                    String errorMessage = "Error during import of feasibility ranges, cause: " + e.getMessage();
                    LOGGER.error(errorMessage);
                    throw new CeMergingException(errorMessage);
                }
            }
            regionFeasibilityRanges = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, task.getProcessTargetDate(), initialRegionNetPositions, feasibilityRange);
        } catch (Exception e) {
            String errorMessage = "Could not calculate feasibility ranges, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private double getCommonLimit(final AlegroData bciAlegroData,
                                  final Map<String, Interval> externalContraintsMapForAlegro) {
        double minEcAlde = externalContraintsMapForAlegro.get(ALEGRO_DE_NODE_NAME).getMinValue();
        double maxEcAlde = externalContraintsMapForAlegro.get(ALEGRO_DE_NODE_NAME).getMaxValue();
        double minEcAlbe = externalContraintsMapForAlegro.get(ALEGRO_BE_NODE_NAME).getMinValue();
        double maxEcAlbe = externalContraintsMapForAlegro.get(ALEGRO_BE_NODE_NAME).getMaxValue();
        double ec;
        if (bciAlegroData.getAlbeFlows().getTargetFlow() < 0) {
            ec = Math.min(Math.abs(maxEcAlbe), Math.abs(minEcAlde));
        } else {
            ec = Math.min(Math.abs(minEcAlbe), Math.abs(maxEcAlde));
        }
        return ec;
    }
}

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
import com.farao_community.farao.ce_merging.common.util.FileUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import static com.farao_community.farao.ce_merging.common.util.FileUtils.readBytesFromPath;
import static java.nio.file.Files.readAllBytes;

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
    private final AlegroData alegroData;

    public BciProcess(final BciTask task,
                      final CeMergingConfiguration configuration,
                      final RegionConfiguration regionConfiguration) {
        this.task = task;
        this.alegroData = Optional.ofNullable(task.getBciInputs().getAlegroNetPositionsPath())
            .map(path -> JsonUtils.read(AlegroData.class, path))
            .orElse(null);
        this.configuration = configuration;
        this.regionConfiguration = regionConfiguration;
    }

    public void run() {
        try {
            importFiles();
            computeBci();
            createBciOutput();
            task.setBciOutput(bciOutput);
        } catch (final Exception e) {
            final String errorMessage = "Error while running BCI";
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private String getInitialNpPath() {
        return task.getBciInputs().getInitialNetPositionsPath();
    }

    private String getExternalConstraintsPath() {
        return task.getBciInputs().getAlegroNetPositionsPath();
    }

    private String getFeasibilityRangePath() {
        return task.getBciInputs().getFeasibilityRangePath();
    }

    private String getNpfPath() {
        return task.getBciInputs().getForecastNetPositionsPath();
    }

    private void importFiles() throws IOException {
        referenceProgram = JsonUtils.read(ReferenceProgram.class, getNpfPath());
        initialGlobalNetPositions = getGlobalNetPosition(getInitialNpPath(), regionConfiguration);
        computeInRegionNetPositions();
        updateAlegroRegionsNetPosition();
        regionFeasibilityRanges = calculateRegionFeasibilityRanges();
    }

    private void computeInRegionNetPositions() {
        Map<String, Double> outRegionNetPositions = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);
        initialGlobalNetPositions.forEach((region, inNp) -> {
            final double outNp = outRegionNetPositions.getOrDefault(region, 0.);
            initialRegionNetPositions.put(region, inNp - outNp);
        });
    }

    private void updateAlegroRegionsNetPosition() throws IOException {
        if (alegroData == null) {
            return;
        }
        updateAlegroRegionNetPosition("BE", alegroData.getAlBeFlows());
        updateAlegroRegionNetPosition("DE", alegroData.getAlDeFlows());
    }

    private void updateAlegroRegionNetPosition(final String countryCode,
                                               final AlegroFlows flows) throws IOException {
        final double alHubToCeFlow = getAlegroConstrainedTargetFlow(flows);
        final double countryAlegroGap = alHubToCeFlow - flows.getInitialFlow();

        final String countryEic = regionConfiguration.getAreasIn().get(countryCode);

        initialRegionNetPositions.computeIfPresent(countryEic, (k, np) -> np + countryAlegroGap);
    }

    private Map<String, Interval> getAlegroExternalConstraints() throws IOException {
        final byte[] externalConstraints = readAllBytes(Paths.get(getExternalConstraintsPath()));
        return calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
    }

    private double getAlegroConstrainedTargetFlow(final AlegroFlows toConstrain) throws IOException {
        final double maxFlow = getCommonLimit(alegroData, getAlegroExternalConstraints());
        return alegroData.isInOutage() ? 0 : Math.clamp(toConstrain.getTargetFlow(), -maxFlow, maxFlow);
    }

    private void computeBci() throws IOException {
        final BciComputation computation = new BciComputation(regionConfiguration, referenceProgram);
        boolean isMergingWithAlegro = alegroData != null;

        final double alDeToCeFlow;
        final double alBeToCeFlow;
        if (isMergingWithAlegro) {
            alDeToCeFlow = getAlegroConstrainedTargetFlow(alegroData.getAlDeFlows());
            alBeToCeFlow = getAlegroConstrainedTargetFlow(alegroData.getAlBeFlows());
        } else {
            alDeToCeFlow = 0;
            alBeToCeFlow = 0;
        }

        final BciComputationResult bciResults = computation.run(regionFeasibilityRanges,
                                                                initialRegionNetPositions,
                                                                alBeToCeFlow,
                                                                alDeToCeFlow);

        processResult = new BciProcessResult(regionConfiguration.getName(),
                                             task.getProcessTargetDate(),
                                             bciResults,
                                             getOutRegionResults(),
                                             getBciAlegroData());
    }

    private BciAlegroData getBciAlegroData() throws IOException {

        if (alegroData.isInOutage()) {
            return null;
        }

        final byte[] externalConstraints = readBytesFromPath(getExternalConstraintsPath());
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

    private Collector<Map.Entry<String, String>, ?, TreeMap<String, Double>> sameKeysValuesFrom(
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

    private void createBciOutput() throws FileNotFoundException {
        final String outputPath = configuration.getOutputsDirectoryPath(task) + File.separator + BCI_OUTPUT_FILE_NAME;
        JsonBciResult.write(processResult, new FileOutputStream(outputPath));
        bciOutput = new BciOutput(outputPath);
    }

    private Map<String, Interval> calculateRegionFeasibilityRanges() {
        final byte[] feasibilityRange = Optional.ofNullable(getFeasibilityRangePath())
            .map(FileUtils::readBytesFromPath)
            .orElse(new byte[0]);

        return new FeasibilityRangeCalculator(regionConfiguration)
            .getRegionFeasibilityRanges(readBytesFromPath(getExternalConstraintsPath()),
                                        task.getProcessTargetDate(),
                                        initialRegionNetPositions,
                                        feasibilityRange);

    }

    private double getCommonLimit(final AlegroData bciAlegroData,
                                  final Map<String, Interval> alegroExternalConstraints) {

        final Interval alDeConstraints = alegroExternalConstraints.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroExternalConstraints.get(ALEGRO_BE_NODE_NAME);

        final boolean flowsToGermany = bciAlegroData.getAlBeFlows().getTargetFlow() < 0;

        final double maxAtOrigin = Math.abs((flowsToGermany ? alBeConstraints : alDeConstraints).getMaxValue());
        // because min < 0
        final double maxAtDestination = Math.abs((flowsToGermany ? alDeConstraints : alBeConstraints).getMinValue());

        return Math.min(maxAtOrigin, maxAtDestination);
    }
}

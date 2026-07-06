/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.base_case_improvement.data.alegro.AlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.data.alegro.AlegroFlows;
import com.farao_community.farao.ce_merging.base_case_improvement.data.alegro.BciAlegroData;
import com.farao_community.farao.ce_merging.base_case_improvement.data.alegro.BciAlegroFlows;
import com.farao_community.farao.ce_merging.base_case_improvement.data.inputs.Interval;
import com.farao_community.farao.ce_merging.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.base_case_improvement.data.result.BciComputationResult;
import com.farao_community.farao.ce_merging.base_case_improvement.data.result.BciProcessResult;
import com.farao_community.farao.ce_merging.base_case_improvement.data.result.JsonBciResult;
import com.farao_community.farao.ce_merging.base_case_improvement.data.result.OutRegionResults;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciOutput;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciTask;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.config.IRegionConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraintsForAlegro;
import static com.farao_community.farao.ce_merging.base_case_improvement.process.InitialNetPositionsImporter.getGlobalNetPosition;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.readBytesFromPath;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(BciProcess.class);
    private static final String BCI_OUTPUT_FILE_NAME = "bciOutput.json";
    private final BciTask task;
    private final IRegionConfiguration regionConfiguration;
    private final CeMergingConfiguration configuration;
    private BciProcessResult processResult;
    private final FlowByAreaMap initialRegionNetPositions = new FlowByAreaMap();
    private FlowByAreaMap initialGlobalNetPositions;
    private Map<String, Interval> regionFeasibilityRanges;
    private ReferenceProgram referenceProgram;
    private BciOutput bciOutput;
    private final AlegroData alegroData;

    public BciProcess(final BciTask task,
                      final CeMergingConfiguration configuration,
                      final IRegionConfiguration regionConfiguration) {
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
        return task.getBciInputs().getExternalConstraintsPath();
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
        final FlowByAreaMap outRegionNetPositions = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);
        initialRegionNetPositions.putAll(
            initialGlobalNetPositions.withValuesShiftedBy(region -> -outRegionNetPositions.getOrZero(region))
        );
    }

    private void updateAlegroRegionsNetPosition() throws IOException {
        if (alegroData == null) {
            return;
        }
        updateAlegroRegionNetPosition("BE", alegroData.getAlbeFlows());
        updateAlegroRegionNetPosition("DE", alegroData.getAldeFlows());
    }

    private void updateAlegroRegionNetPosition(final String countryCode,
                                               final AlegroFlows flows) throws IOException {
        final double alHubToCeFlow = getAlegroConstrainedTargetFlow(flows);
        final double countryAlegroGap = alHubToCeFlow - flows.getInitialFlow();
        final String countryEic = regionConfiguration.getAreaInEic(countryCode);

        initialRegionNetPositions.shiftFlow(countryEic, countryAlegroGap);
    }

    private Map<String, Interval> getAlegroExternalConstraints() throws IOException {
        final byte[] externalConstraints = readBytesFromPath(getExternalConstraintsPath());
        return calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
    }

    private double getAlegroConstrainedTargetFlow(final AlegroFlows toConstrain) throws IOException {
        if (alegroData.isAlegroInOutage()) {
            return 0;
        }
        final double maxAlegroFlow = getCommonFlowLimit(alegroData, getAlegroExternalConstraints());
        return Math.clamp(toConstrain.getTargetFlow(), -maxAlegroFlow, maxAlegroFlow);
    }

    private void computeBci() throws IOException {
        final BciComputation computation = new BciComputation(regionConfiguration, referenceProgram, regionFeasibilityRanges);

        final double alDeToCeFlow = shouldIgnoreAlegro() ? 0 : getAlegroConstrainedTargetFlow(alegroData.getAldeFlows());
        final double alBeToCeFlow = shouldIgnoreAlegro() ? 0 : getAlegroConstrainedTargetFlow(alegroData.getAlbeFlows());

        final BciComputationResult bciResults = computation.run(initialRegionNetPositions,
                                                                alBeToCeFlow,
                                                                alDeToCeFlow);

        processResult = new BciProcessResult(regionConfiguration.getName(),
                                             task.getProcessTargetDate(),
                                             bciResults,
                                             getOutRegionResults(),
                                             getBciAlegroData());
    }

    private BciAlegroData getBciAlegroData() {

        if (shouldIgnoreAlegro()) {
            return null;
        }

        final byte[] externalConstraints = readBytesFromPath(getExternalConstraintsPath());
        final Map<String, Interval> alegroEc = calculateConstraintsForAlegro(externalConstraints, task.getProcessTargetDate());
        final Interval alDeConstraints = alegroEc.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroEc.get(ALEGRO_BE_NODE_NAME);

        final BciAlegroFlows alDeFlows = new BciAlegroFlows(alegroData.getAldeFlows().getTargetFlow(), alDeConstraints);
        final BciAlegroFlows alBeFlows = new BciAlegroFlows(alegroData.getAlbeFlows().getTargetFlow(), alBeConstraints);

        return new BciAlegroData(alDeFlows, alBeFlows);
    }

    private OutRegionResults getOutRegionResults() {
        final FlowByAreaMap globalNetPositionsByAreaId = referenceProgram
            .computeGlobalNetPositionsForOutAreas(regionConfiguration);

        final Map<String, Double> globalNpByCountry = regionConfiguration
            .getAreasOut()
            .entrySet()
            .stream()
            .collect(toMap(Map.Entry::getKey,
                           e -> globalNetPositionsByAreaId.getOrZero(e.getValue()),
                           (o1, o2) -> o1,
                           TreeMap::new));

        return new OutRegionResults(globalNpByCountry);
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

    private double getCommonFlowLimit(final AlegroData bciAlegroData,
                                      final Map<String, Interval> alegroExternalConstraints) {

        final Interval alDeConstraints = alegroExternalConstraints.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroExternalConstraints.get(ALEGRO_BE_NODE_NAME);

        final boolean flowsToGermany = bciAlegroData.getAlbeFlows().getTargetFlow() < 0;

        final Interval atOrigin = flowsToGermany ? alBeConstraints : alDeConstraints;
        final Interval atDestination = flowsToGermany ? alDeConstraints : alBeConstraints;

        return min(abs(atOrigin.getMaxValue()),
                   abs(atDestination.getMinValue()));
    }

    private boolean shouldIgnoreAlegro() {
        return alegroData == null || alegroData.isAlegroInOutage();
    }
}

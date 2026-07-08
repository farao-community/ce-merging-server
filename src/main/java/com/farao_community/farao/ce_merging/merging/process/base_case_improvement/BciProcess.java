/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciProcessResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.OutRegionResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.BciComputation;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.FeasibilityRangeCalculator;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.readBytesFromPath;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraintsForAlegro;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.InitialNetPositionsImporter.getGlobalNetPosition;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciProcess {
    private static final Logger LOGGER = LoggerFactory.getLogger(BciProcess.class);
    private final MergingTask task;
    private final RegionConfiguration regionConfiguration;
    private final CeMergingConfiguration configuration;
    private final FlowByAreaMap initialRegionNetPositions = new FlowByAreaMap();
    private final byte[] externalConstraintsBytes;
    private final byte[] feasibilityRangesBytes;
    private BciProcessResult processResult;
    private FlowByAreaMap initialGlobalNetPositions;
    private Map<String, Interval> regionFeasibilityRanges;
    private ReferenceProgram referenceProgram;
    private final AlegroData alegroData;

    public BciProcess(final MergingTask task,
                      final CeMergingConfiguration configuration) {
        this.task = task;
        this.regionConfiguration = task.getConfigurations().getRegionConfiguration();
        this.alegroData = Optional.ofNullable(task.getArtifactPath(ALEGRO_NET_POSITIONS))
            .map(path -> JsonUtils.read(AlegroData.class, path))
            .orElse(null);
        this.configuration = configuration;
        this.externalConstraintsBytes = readBytesFromPath(getExternalConstraintsPath());
        this.feasibilityRangesBytes =  Optional.ofNullable(getFeasibilityRangePath())
            .map(FileUtils::readBytesFromPath)
            .orElse(new byte[0]);

    }

    public void run() {
        try {
            importFiles();
            computeBci();
            saveInArtifacts();
        } catch (final Exception e) {
            final String errorMessage = "Error while running BCI";
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private String getInitialNpPath() {
        return task.getArtifactPath(IGMS_NET_POSITIONS_FILE);
    }

    private String getExternalConstraintsPath() {
        return task.getInputs().getExternalConstraints().getPath();
    }

    private String getFeasibilityRangePath() {
        return task.getInputs().getFeasibilityRanges().getPath();
    }

    private String getNpfPath() {
        return task.getArtifactPath(REFERENCE_PROGRAM_FORECAST_FILE);
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
        updateAlegroRegionNetPosition("BE", alegroData.albeFlows());
        updateAlegroRegionNetPosition("DE", alegroData.aldeFlows());
    }

    private void updateAlegroRegionNetPosition(final String countryCode,
                                               final AlegroFlows flows) throws IOException {
        final double alHubToCeFlow = getAlegroConstrainedTargetFlow(flows);
        final double countryAlegroGap = alHubToCeFlow - flows.initialFlow();
        final String countryEic = regionConfiguration.getAreaInEic(countryCode);

        initialRegionNetPositions.shiftFlow(countryEic, countryAlegroGap);
    }

    private Map<String, Interval> getAlegroExternalConstraints() {
        return calculateConstraintsForAlegro(externalConstraintsBytes, task.getTargetDate());
    }

    private double getAlegroConstrainedTargetFlow(final AlegroFlows toConstrain) throws IOException {
        if (alegroData.alegroInOutage()) {
            return 0;
        }
        final double maxAlegroFlow = getCommonFlowLimit(alegroData, getAlegroExternalConstraints());
        return Math.clamp(toConstrain.targetFlow(), -maxAlegroFlow, maxAlegroFlow);
    }

    private void computeBci() throws IOException {
        final BciComputation computation = new BciComputation(regionConfiguration, referenceProgram, regionFeasibilityRanges);

        final double alDeToCeFlow = shouldIgnoreAlegro() ? 0 : getAlegroConstrainedTargetFlow(alegroData.aldeFlows());
        final double alBeToCeFlow = shouldIgnoreAlegro() ? 0 : getAlegroConstrainedTargetFlow(alegroData.albeFlows());

        final BciComputationResult bciResults = computation.run(initialRegionNetPositions,
                                                                alBeToCeFlow,
                                                                alDeToCeFlow);

        processResult = new BciProcessResult(regionConfiguration.getName(),
                                             task.getTargetDate(),
                                             bciResults,
                                             getOutRegionResults(),
                                             getBciAlegroData());
    }

    private BciAlegroData getBciAlegroData() {

        if (shouldIgnoreAlegro()) {
            return null;
        }

        final byte[] externalConstraints = readBytesFromPath(getExternalConstraintsPath());
        final Map<String, Interval> alegroEc = calculateConstraintsForAlegro(externalConstraints, task.getTargetDate());
        final Interval alDeConstraints = alegroEc.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroEc.get(ALEGRO_BE_NODE_NAME);

        final BciAlegroFlows alDeFlows = new BciAlegroFlows(alegroData.aldeFlows().targetFlow(), alDeConstraints);
        final BciAlegroFlows alBeFlows = new BciAlegroFlows(alegroData.albeFlows().targetFlow(), alBeConstraints);

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

    private Map<String, Interval> calculateRegionFeasibilityRanges() {
        return new FeasibilityRangeCalculator(regionConfiguration)
            .getRegionFeasibilityRanges(readBytesFromPath(getExternalConstraintsPath()),
                                        task.getTargetDate(),
                                        initialRegionNetPositions,
                                        feasibilityRangesBytes);

    }

    private double getCommonFlowLimit(final AlegroData bciAlegroData,
                                      final Map<String, Interval> alegroExternalConstraints) {

        final Interval alDeConstraints = alegroExternalConstraints.get(ALEGRO_DE_NODE_NAME);
        final Interval alBeConstraints = alegroExternalConstraints.get(ALEGRO_BE_NODE_NAME);

        final boolean flowsToGermany = bciAlegroData.albeFlows().targetFlow() < 0;

        final Interval atOrigin = flowsToGermany ? alBeConstraints : alDeConstraints;
        final Interval atDestination = flowsToGermany ? alDeConstraints : alBeConstraints;

        return min(abs(atOrigin.getMaxValue()),
                   abs(atDestination.getMinValue()));
    }

    private boolean shouldIgnoreAlegro() {
        return alegroData == null || alegroData.alegroInOutage();
    }

    private void saveInArtifacts() {
        final String fileName = "bciOutputs.json";
        try {
            final Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(task), fileName);
            Files.createFile(filePath);
            JsonUtils.writeInPath(BciProcessResult.class, processResult, filePath);
            final SavedFile savedFile = new SavedFile(fileName,
                                                      filePath.toString(),
                                                      String.format("/tasks/%d/artifacts/bci-output",
                                                                    task.getId()));
            task.getArtifacts().putFile(BCI_OUTPUT_FILE, savedFile);
            LOGGER.info("file '{}' is saved in task '{}' artifacts", fileName, task.getId());
        } catch (Exception e) {
            LOGGER.error("Cannot write file '{}' in task '{}' artifacts", fileName, task.getId(), e);
            throw new CeMergingException(String.format("Cannot write file '%s' in task '%d' artifacts", fileName, task.getId()), e);
        }
    }
}

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
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciProcessResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.OutRegionResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.BciComputer;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.FeasibilityRangeCalculator;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.readBytesFromPath;
import static com.farao_community.farao.ce_merging.merging.process.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraintsForAlegro;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.InitialNetPositionsImporter.getGlobalNetPosition;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(BciProcessor.class);
    private final MergingTask task;
    private final RegionConfiguration regionConfiguration;
    private final CeMergingConfiguration configuration;
    private final FlowByAreaMap initialRegionNetPositions = new FlowByAreaMap();
    private final byte[] feasibilityRangesBytes;
    private final Map<String, Interval> alegroConstraints;
    private BciProcessResult processResult;
    private FlowByAreaMap initialGlobalNetPositions;
    private Map<String, Interval> regionFeasibilityRanges;
    private ReferenceProgram referenceProgram;
    private final AlegroData alegroData;

    public BciProcessor(final MergingTask task,
                        final CeMergingConfiguration configuration) {
        this.task = task;
        this.regionConfiguration = task.getConfigurations().getRegionConfiguration();
        this.alegroData = Optional.ofNullable(task.getArtifactPath(ALEGRO_NET_POSITIONS))
            .map(path -> JsonUtils.read(AlegroData.class, path))
            .orElse(null);
        this.configuration = configuration;
        this.alegroConstraints = calculateConstraintsForAlegro(readBytesFromPath(getExternalConstraintsPath()),
                                                               task.getTargetDate());
        this.feasibilityRangesBytes =  Optional.ofNullable(getFeasibilityRangePath())
            .map(FileUtils::readBytesFromPath)
            .orElse(new byte[0]);

    }

    public void run() {
        try {
            importFiles();
            computeBci();
            saveArtifactFile(BCI_OUTPUT_FILE, processResult, task, configuration);
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
        final FlowByAreaMap outRegionNetPositions = referenceProgram.getAllNetPositionsOutRegion(regionConfiguration);
        initialRegionNetPositions.putAll(
            initialGlobalNetPositions.withValuesShiftedBy(region -> -outRegionNetPositions.getOrZero(region))
        );
    }

    private void updateAlegroRegionsNetPosition() {
        if (alegroData == null) {
            return;
        }
        updateAlegroRegionNetPosition("BE", alegroData.albeFlows());
        updateAlegroRegionNetPosition("DE", alegroData.aldeFlows());
    }

    private void updateAlegroRegionNetPosition(final String countryCode,
                                               final AlegroFlows flows) {
        final double alegroToCeFlow = getAlegroConstrainedTargetFlow(flows);
        final double countryAlegroGap = alegroToCeFlow - flows.initialFlow();
        final String countryEic = regionConfiguration.getAreaInEic(countryCode);

        initialRegionNetPositions.shiftFlow(countryEic, countryAlegroGap);
    }

    private double getAlegroConstrainedTargetFlow(final AlegroFlows toConstrain) {
        if (alegroData.alegroInOutage()) {
            return 0;
        }
        final double maxAlegroFlow = getCommonFlowLimit();
        return Math.clamp(toConstrain.targetFlow(), -maxAlegroFlow, maxAlegroFlow);
    }

    private void computeBci() {
        final BciComputer computer = new BciComputer(regionConfiguration, referenceProgram, regionFeasibilityRanges);

        final double alegroGermanyToCeFlow = alegroOutOrNoData() ? 0 : getAlegroConstrainedTargetFlow(alegroData.aldeFlows());
        final double alegroBelgiumToCeFlow = alegroOutOrNoData() ? 0 : getAlegroConstrainedTargetFlow(alegroData.albeFlows());

        final BciComputationResult bciResults = computer.run(initialRegionNetPositions,
                                                             alegroBelgiumToCeFlow,
                                                             alegroGermanyToCeFlow);

        processResult = new BciProcessResult(regionConfiguration.getName(),
                                             task.getTargetDate(),
                                             bciResults,
                                             getOutRegionResults(),
                                             getBciAlegroData());
    }

    private BciAlegroData getBciAlegroData() {

        if (alegroOutOrNoData()) {
            return null;
        }

        final Interval alDeConstraints = getGermanAlegroConstraint();
        final Interval alBeConstraints = getBelgianAlegroConstraint();

        final BciAlegroFlows alDeFlows = new BciAlegroFlows(alegroData.aldeFlows(), alDeConstraints);
        final BciAlegroFlows alBeFlows = new BciAlegroFlows(alegroData.albeFlows(), alBeConstraints);

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

    private double getCommonFlowLimit() {

        final Interval alDeConstraints = getGermanAlegroConstraint();
        final Interval alBeConstraints = getBelgianAlegroConstraint();

        final boolean flowsToGermany = alegroData.albeFlows().targetFlow() < 0;

        final Interval outAreaBoundaries = flowsToGermany ? alBeConstraints : alDeConstraints;
        final Interval inAreaBoundaries = flowsToGermany ? alDeConstraints : alBeConstraints;

        return min(abs(outAreaBoundaries.getMaxValue()),
                   abs(inAreaBoundaries.getMinValue()));
    }

    private boolean alegroOutOrNoData() {
        return alegroData == null || alegroData.alegroInOutage();
    }

    private Interval getBelgianAlegroConstraint() {
        return alegroConstraints.get(ALEGRO_BE_NODE_NAME);
    }

    private Interval getGermanAlegroConstraint() {
        return alegroConstraints.get(ALEGRO_DE_NODE_NAME);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.german_pre_merge;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.process.netpositions.NetPositionService;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isPairedWithVirtualHub;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getBorderFlow;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getComponentMode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_IGMS_NET_POSITIONS_FILE;
import static com.powsybl.iidm.network.Country.DE;
import static java.lang.Math.abs;
import static java.util.function.Predicate.not;

@Service
public class GermanMismatchCompensationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GermanMismatchCompensationService.class);
    private final NetPositionService netPositionService;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private final CeMergingConfiguration configuration;

    public GermanMismatchCompensationService(final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier,
                                             final NetPositionService netPositionService,
                                             final CeMergingConfiguration configuration) {
        this.netPositionService = netPositionService;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
        this.configuration = configuration;
    }

    private static Predicate<DanglingLine> isGermanExternalNode(final List<XnodeConfig> xnodesConfigList) {

        return danglingLine -> xnodesConfigList.stream()
                .filter(xnode -> xnode.getName().equals(danglingLine.getPairingKey()))
                .findFirst()
                .filter(isNotDeOnBothSides().or(isLinkedToDenmark()))
                .isPresent();
    }

    private static Predicate<XnodeConfig> isNotDeOnBothSides() {
        return xnode -> !DE.name().equals(xnode.getArea1()) || !DE.name().equals(xnode.getArea2());
    }

    private static Predicate<XnodeConfig> isLinkedToDenmark() {
        return xnode -> DANISH_TSO.equals(xnode.getSubarea1()) || DANISH_TSO.equals(xnode.getSubarea2());
    }

    public void apply(final MergingTask task,
                      final Network mergedNetwork) {
        try {
            LOGGER.info("Mismatch compensation on german network {}", mergedNetwork.getNameOrId());
            final NetPositionsResults germanNetPositionResults = netPositionService.computeGermanNetPositions(task);
            saveArtifactFile(GERMAN_IGMS_NET_POSITIONS_FILE, germanNetPositionResults, task, configuration);
            final Configurations config = task.getConfigurations();
            final double initialInternalNPs = getInitialInternalNetPosition(germanNetPositionResults);
            final LoadFlowParameters lfParams = config.getLoadFlowParameters();
            runLoadFlowWithBalanceTypeCorrection(mergedNetwork, loadFlowRunnerSupplier, lfParams);
            compensateGermanMismatch(mergedNetwork,
                                     initialInternalNPs,
                                     config.getVirtualHubList(),
                                     config.getXnodeList(),
                                     getComponentMode(lfParams));
        } catch (final Exception e) {
            String errorMessage = "Mismatch compensation failed, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    double getInitialInternalNetPosition(final NetPositionsResults germanNetPositions) {
        return Stream.of(GermanTso.values())
                .map(GermanTso::name)
                .map(germanNetPositions::get)
                .filter(Objects::nonNull)
                .map(NetPositions::getGlobalDetailedExchanges)
                .mapToDouble(ex -> ex.getOrDefault(DE.name(), 0.))
                .sum();
    }

    private void compensateGermanMismatch(final Network network,
                                          final double mismatch,
                                          final List<VirtualHubRecord> virtualHubs,
                                          final List<XnodeConfig> xnodes,
                                          final LoadFlowParameters.ComponentMode componentMode) {

        final List<DanglingLine> externalDanglingLines = network.getDanglingLineStream()
                .filter(isGermanExternalNode(xnodes).and(NetworkUtil::hasActivePower).and(not(isPairedWithVirtualHub(virtualHubs))))
                .toList();

        LOGGER.info("Absolute proportional share is applied to compensate German mismatch ({} MW)", mismatch);
        final double totalExternalNetPosition = externalDanglingLines.stream()
                .map(line -> getBorderFlow(line, componentMode))
                .mapToDouble(Math::abs)
                .sum();

        LOGGER.info("Sum of external Net positions for Germany is {} MW", totalExternalNetPosition);
        if (totalExternalNetPosition != 0) {
            externalDanglingLines.forEach(line -> updateBoundaryLineFlow(line, mismatch, totalExternalNetPosition, componentMode));
        } else {
            LOGGER.warn("German dangling lines not updated because total external NP = 0");
        }
    }

    private void updateBoundaryLineFlow(final DanglingLine line,
                                        final double mismatch,
                                        final double totalExternalNetPosition,
                                        final LoadFlowParameters.ComponentMode componentMode) {
        final double initialFlow = getBorderFlow(line, componentMode);
        final double proportionOfMismatch = mismatch * abs(initialFlow) / totalExternalNetPosition;
        double updatedFlow = initialFlow + proportionOfMismatch;
        LOGGER.info("Flow on {} updated from {} to {} MW", line.getId(), initialFlow, updatedFlow);

        line.setP0(updatedFlow);

        if (line.getGeneration() != null) {
            LOGGER.info("Active Generation is set to zero for dangling line {}", line.getId());
            line.getGeneration().setTargetP(0);
        }
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.virtuals_hubs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.CountryUtils;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.forecast_netpositions.ReferenceExchangeData;
import com.farao_community.farao.ce_merging.merging.process.forecast_netpositions.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;

public class VirtualHubsShifting {
    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualHubsShifting.class);

    public static Map<String, Double> applyVirtualHubFlows(final MergingTask task, final CeMergingConfiguration configuration) throws FileNotFoundException {
        Map<String, Double> virtualHubsGaps = new HashMap<>();
        final ReferenceProgram referenceProgram = JsonUtils.read(ReferenceProgram.class, task.getArtifacts().getFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE).getPath());
        final List<VirtualHubRecord> virtualHubRecords = task.getConfigurations().getVirtualHubList();
        final String tgmPath = task.getArtifacts().getFile(ArtifactType.TGM_FILE_AFTER_RECESSIVITY).getPath();
        final Network network = Network.read(tgmPath);
        referenceProgram.getReferenceExchangeDataList()
                .forEach(referenceExchangeData -> {
                    final VirtualHubRecord virtualHubRecord = findVirtualHub(virtualHubRecords, referenceExchangeData.getAreaOutId(), referenceExchangeData.getAreaInId());
                    final String nodeName = virtualHubRecord.getNodeName();
                    final DanglingLine danglingLine = findDanglingLine(network, nodeName);
                    if (danglingLine.getTerminal().isConnected()) {
                        final double initialFlow = danglingLine.getP0();
                        final double targetFlow = computeTargetFlow(virtualHubRecord, referenceExchangeData);
                        final double virtualHubFlowGap = targetFlow - initialFlow;
                        if (virtualHubFlowGap != 0.0) {
                            final String country = CountryUtils.mapDk1ToDk(virtualHubRecord.getRelatedMaCode());
                            virtualHubsGaps.put(country, virtualHubFlowGap);
                            setDanglingLineFlow(danglingLine, targetFlow);
                            LOGGER.info("Shift virtual hub {}: {} -> {} (gap={})", nodeName, initialFlow, targetFlow, virtualHubFlowGap);
                        }
                    }
                });
        FileStorageUtils.saveArtifactNetwork(ArtifactType.TGM_FILE_AFTER_RECESSIVITY, network, task, UCTE_FORMAT, configuration);
        return virtualHubsGaps;
    }

    private static double computeTargetFlow(final VirtualHubRecord virtualHub, final ReferenceExchangeData exchange) {
        return virtualHub.getEic().equals(exchange.getAreaInId())
                ? exchange.getFlow()
                : -exchange.getFlow();
    }

    private static VirtualHubRecord findVirtualHub(final List<VirtualHubRecord> virtualHubRecords, final String areaOut, final String areaIn) {
        return virtualHubRecords.stream()
                .filter(virtualHubRecord -> virtualHubRecord.getEic().equals(areaOut) || virtualHubRecord.getEic().equals(areaIn))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find exchange from " + areaOut + " to " + areaIn + " in virtualHubs"));
    }

    private static DanglingLine findDanglingLine(final Network network, final String nodeName) {
        return network.getDanglingLineStream()
                .filter(danglingLine -> nodeName.equals(danglingLine.getPairingKey()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find dangling line for node " + nodeName + " in network " + network.getId()));
    }

    private static void setDanglingLineFlow(final DanglingLine danglingLine, final double targetFlow) {
        danglingLine.setP0(targetFlow);
        danglingLine.getGeneration().setTargetP(0.0);
    }
}

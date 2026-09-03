/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;

@Service
public class TsoInformationsService {
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    public TsoInformationsService(Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }

    public List<ReportCommonsInformation> calculateTsoInformations(final MergingTask task) {
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        final LoadFlowParameters.ComponentMode componentModeLfParameter = LoadFlowUtils.getComponentMode(loadFlowParameters);
        final Network cgmNetwork = Network.read(task.getArtifactPath(ArtifactType.CGM_FILE_AFTER_PST));
        LoadFlowUtils.runLoadFlow(cgmNetwork, loadFlowRunnerSupplier, loadFlowParameters);
        List<ReportCommonsInformation> reportCommonsInformationList = new ArrayList<>();
        List<VirtualHubRecord> virtualHubList = task.getConfigurations().getVirtualHubList();
        Set<String> germanZones = task.getConfigurations().getRegionConfiguration().getGermanyZone().keySet();
        NetPositionsResults germanNetPositions = JsonUtils.read(NetPositionsResults.class, task.getArtifactPath(ArtifactType.GERMAN_IGMS_NET_POSITIONS_FILE));
        germanZones.forEach(zone -> reportCommonsInformationList.add(
                createReportCommonsInformationForZone(germanNetPositions, cgmNetwork, zone, componentModeLfParameter, virtualHubList)));
        return reportCommonsInformationList;
    }

    private ReportCommonsInformation createReportCommonsInformationForZone(final NetPositionsResults germanNetPositions, final Network cgmNetwork, final String zone, final LoadFlowParameters.ComponentMode componentModeLfParameter, List<VirtualHubRecord> virtualHubList) {
        double generationIgm = 0.;
        double loadIgm = 0.;
        double globalBalanceIgm = 0.;
        NetPositions netPositions = germanNetPositions.netPositionsByCountryMap().get(zone);
        if (netPositions != null) {
            generationIgm = netPositions.getGenerationAndLoadQuantity().generation();
            loadIgm = netPositions.getGenerationAndLoadQuantity().load();
            globalBalanceIgm = netPositions.getGlobalNetPosition().getWithoutVirtualHubs();
        }
        final double generationCgm = getCgmGenerationQuantity(cgmNetwork, zone, componentModeLfParameter);
        final double loadCgm = getCgmLoadQuantity(cgmNetwork, zone, componentModeLfParameter);
        final double globalBalanceCgm = getTsoGlobalNetPosition(cgmNetwork, zone, componentModeLfParameter, virtualHubList);
        return new ReportCommonsInformation(zone, generationIgm, loadIgm, globalBalanceIgm, generationCgm, loadCgm, globalBalanceCgm);
    }

    private double getTsoGlobalNetPosition(final Network network, final String zone, final LoadFlowParameters.ComponentMode componentModeLfParameter, final List<VirtualHubRecord> virtualHubList) {
        double globalNetPositionWithoutVirtualHubs = network.getDanglingLineStream()
                .filter(danglingLine -> NetworkUtil.isBorderOfZone(danglingLine, zone) && !NetworkUtil.isPairedWithVirtualHub(danglingLine, virtualHubList))
                .mapToDouble(danglingLine -> NetworkUtil.getBorderFlow(danglingLine, componentModeLfParameter))
                .sum();

        globalNetPositionWithoutVirtualHubs += network.getLineStream()
                .filter(line -> NetworkUtil.isBorderOfZone(line, zone))
                .mapToDouble(line -> NetworkUtil.getBorderFlow(line, zone))
                .sum();

        return globalNetPositionWithoutVirtualHubs;
    }

    private double getCgmLoadQuantity(final Network network, final String zone, final LoadFlowParameters.ComponentMode componentModeLfParameter) {
        return network.getLoadStream()
                .filter(load -> belongsToZone(load.getNameOrId(), zone))
                .filter(isConnected(componentModeLfParameter))
                .mapToDouble(load -> Double.isNaN(load.getTerminal().getP()) ? 0 : load.getTerminal().getP())
                .sum();
    }

    private double getCgmGenerationQuantity(final Network network, final String zone, final LoadFlowParameters.ComponentMode componentModeLfParameter) {
        return network.getGeneratorStream()
                .filter(generator -> belongsToZone(generator.getNameOrId(), zone))
                .filter(isConnected(componentModeLfParameter))
                .mapToDouble(generator -> Double.isNaN(generator.getTerminal().getP()) ? 0 : -generator.getTerminal().getP())
                .sum();
    }

    private boolean belongsToZone(final String nameOrId, final String zone) {
        return nameOrId != null && nameOrId.length() >= 2 && zone.equals(nameOrId.substring(0, 2));
    }

}

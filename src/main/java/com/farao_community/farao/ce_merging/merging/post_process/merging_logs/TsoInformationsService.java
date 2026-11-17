/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getBorderFlow;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isBorderOfZone;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isPairedWithVirtualHub;
import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sum;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_IGMS_NET_POSITIONS_FILE;

@Service
public class TsoInformationsService {
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    public TsoInformationsService(final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }

    public List<ReportCommonsInformation> calculateTsoInformations(final MergingTask task) throws FileNotFoundException {
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        final Network cgmNetwork = Network.read(task.getArtifactPath(ArtifactType.CGM_FILE_AFTER_PST));
        LoadFlowUtils.runLoadFlow(cgmNetwork, loadFlowRunnerSupplier, loadFlowParameters);

        NetPositionsResults germanNetPositions = task.getArtifact(GERMAN_IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);

        return task.getConfigurations().getRegionConfiguration().getGermanyZone().keySet().stream()
                .map(zone -> createReportCommonsInformationForZone(germanNetPositions,
                                                                   cgmNetwork,
                                                                   zone,
                                                                   LoadFlowUtils.getComponentMode(loadFlowParameters),
                                                                   task.getConfigurations().getVirtualHubList()))
                .toList();
    }

    private ReportCommonsInformation createReportCommonsInformationForZone(final NetPositionsResults germanNetPositions,
                                                                           final Network cgmNetwork,
                                                                           final String zone,
                                                                           final LoadFlowParameters.ComponentMode componentMode,
                                                                           List<VirtualHubRecord> virtualHubList) {
        double generationIgm = 0.;
        double loadIgm = 0.;
        double globalBalanceIgm = 0.;
        NetPositions netPositions = germanNetPositions.netPositionsByCountryMap().get(zone);
        if (netPositions != null) {
            generationIgm = netPositions.getGenerationAndLoadQuantity().generation();
            loadIgm = netPositions.getGenerationAndLoadQuantity().load();
            globalBalanceIgm = netPositions.getGlobalNetPosition().getWithoutVirtualHubs();
        }
        final double generationCgm = getCgmGenerationQuantity(cgmNetwork, zone, componentMode);
        final double loadCgm = getCgmLoadQuantity(cgmNetwork, zone, componentMode);
        final double globalBalanceCgm = getTsoGlobalNetPosition(cgmNetwork, zone, componentMode, virtualHubList);
        return new ReportCommonsInformation(zone, generationIgm, loadIgm, globalBalanceIgm, generationCgm, loadCgm, globalBalanceCgm);
    }

    private double getTsoGlobalNetPosition(final Network network,
                                           final String zone,
                                           final LoadFlowParameters.ComponentMode componentMode,
                                           final List<VirtualHubRecord> virtualHubList) {

        return sum(network.getBoundaryLineStream(), bl -> getBorderFlow(bl, componentMode), isPairedWithVirtualHub(virtualHubList).and(isBorderOfZone(zone)))
               + sum(network.getLineStream(), l -> NetworkUtil.getBorderFlow(l, zone), isBorderOfZone(zone));
    }

    private double getCgmLoadQuantity(final Network network,
                                      final String zone,
                                      final LoadFlowParameters.ComponentMode componentMode) {
        return sum(network.getLoadStream(),
                   NetworkUtil::getInjectedPower,
                   belongsToZone(zone).and(isConnected(componentMode)));
    }

    private double getCgmGenerationQuantity(final Network network,
                                            final String zone,
                                            final LoadFlowParameters.ComponentMode componentMode) {
        return sum(network.getGeneratorStream(),
                   NetworkUtil::getInjectedPower,
                   belongsToZone(zone).and(isConnected(componentMode)));
    }

    private <T extends Injection<?>> Predicate<T> belongsToZone(final String zone) {
        return i -> belongsToZone(i.getNameOrId(), zone);
    }

    private boolean belongsToZone(final String nameOrId,
                                  final String zone) {
        return nameOrId != null && nameOrId.length() >= 2 && zone.equals(nameOrId.substring(0, 2));
    }

}

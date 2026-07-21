/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.monita;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Properties;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isBorderOf;
import static com.powsybl.iidm.network.Country.IT;
import static com.powsybl.iidm.network.Country.ME;

@Service
public class MonitaService {

    private static final String UCTE_EXPORT_NAMING_STRATEGY_PROPERTY = "ucte.export.naming-strategy";
    private static final String MONITA_NAMING_STRATEGY = "MonitaNamingStrategy";
    private static final String MONITA1_ME_NODE_NAME = "XKOTR120";
    private static final String MONITA2_ME_NODE_NAME = "XKOTR220";

    private final CeMergingConfiguration configuration;

    public MonitaService(final CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void renameNode(final MergingTask task) {
        final Properties properties = new Properties();
        properties.setProperty(UCTE_EXPORT_NAMING_STRATEGY_PROPERTY, MONITA_NAMING_STRATEGY);
        FileStorageUtils.savePreTreatedIgm(IT,
                                           getItalianIgm(task),
                                           properties,
                                           UCTE_FORMAT,
                                           "igms-after-monita-renaming",
                                           task,
                                           configuration);
    }

    private static Network getItalianIgm(final MergingTask task) {
        return Network.read(Optional.ofNullable(task.getArtifacts().getPreTreatedIgm(IT))
                                .orElse(task.getInputs().getIgm(IT).getIgmFile()).getPath());
    }

    /*
        MONITA nodes are in the IT igm ; we move them to ME in order to correct
        the outBciNetPosition value of ME in the igmsNetPositions.json file.
        This will allow us to correct the target value for ME in the balancesAdjustmentTarget.json file.
     */
    public static void postTreatmentForMonita(final MergingTask task, final NetPositionsResults netPositionsFile) {
        final NetPositions italyNetPosition = netPositionsFile.get(IT);
        final NetPositions montenegroNetPosition = netPositionsFile.get(ME);

        if (italyNetPosition == null || montenegroNetPosition == null) {
            return;
        }

        final Network italy = getItalianIgm(task);

        // Italy
        removeFromItaly(MONITA1_ME_NODE_NAME, italyNetPosition, italy);
        removeFromItaly(MONITA2_ME_NODE_NAME, italyNetPosition, italy);

        // Montenegro
        addToMontenegroIfFound(MONITA1_ME_NODE_NAME, montenegroNetPosition, italy);
        addToMontenegroIfFound(MONITA2_ME_NODE_NAME, montenegroNetPosition, italy);
    }

    private static void addToMontenegroIfFound(final String monitaNode,
                                               final NetPositions montenegroNp,
                                               final Network italy) {
        italy.getDanglingLineStream()
            .filter(isBorderOf(IT))
            .filter(l -> l.getPairingKey().equals(monitaNode))
            .findFirst()
            .map(DanglingLine::getP0)
            .ifPresent(monitaFlow -> {
                final double initialGlobal = montenegroNp.getGlobalNetPosition().getWithVirtualHubs();
                montenegroNp.getGlobalNetPosition().setWithVirtualHubs(initialGlobal + monitaFlow);

                final double initialOutBci = montenegroNp.getOutBciNetPosition();
                montenegroNp.setOutBciNetPosition(initialOutBci + monitaFlow);

                final double flowUpdated = montenegroNp.getVirtualHubFlow(monitaNode) + monitaFlow;
                montenegroNp.getVirtualHubsExchanges().put(monitaNode, flowUpdated);
            });
    }

    private static void removeFromItaly(final String monitaNode, final NetPositions italianNetPositions, final Network italy) {
        final double virtualHubFlow = italianNetPositions.getVirtualHubFlow(monitaNode);

        final double globalNetPosition = italianNetPositions.getGlobalNetPosition().getWithVirtualHubs();
        italianNetPositions.getGlobalNetPosition().setWithVirtualHubs(globalNetPosition - virtualHubFlow);

        final double outBciNetPosition = italianNetPositions.getOutBciNetPosition();
        italianNetPositions.setOutBciNetPosition(outBciNetPosition - virtualHubFlow);

        italianNetPositions.getVirtualHubsExchanges().remove(monitaNode);
    }
}

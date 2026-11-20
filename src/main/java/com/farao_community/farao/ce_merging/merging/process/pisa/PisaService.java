/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pisa;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PISA_NAMING_STRATEGY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_EXPORT_NAMING_STRATEGY_PROPERTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFileWithWriter;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getNodeName;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInCountry;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInOutage;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isPairedWith;
import static com.farao_community.farao.ce_merging.merging.process.pisa.PisaNamingStrategy.FFG_NODE_NAME_PATTERN;
import static com.farao_community.farao.ce_merging.merging.process.pisa.PisaNamingStrategy.XPI_NODE_NAME_PATTERN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PISA_RENAMING;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.Country.IT;
import static org.apache.commons.lang3.ObjectUtils.anyNull;

@Service
public class PisaService {
    private final CeMergingConfiguration configuration;
    private static final Map<String, String> CURRENT_TO_FUTURE_ITA_HUBS = Map.of("XPI_GI11", "XFG.IL11",
                                                                             "XPI_GI12", "XFG.IL13");

    private static final Map<String, String> CURRENT_TO_FUTURE_FR_HUBS = Map.of("XPI_GI11", "XFG.IL12",
                                                                                 "XPI_GI12", "XFG.IL14");

    public PisaService(final CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void displacePisaNodes(final MergingTask task) throws FileNotFoundException {
        final Properties properties = new Properties();
        properties.setProperty(UCTE_EXPORT_NAMING_STRATEGY_PROPERTY, PISA_NAMING_STRATEGY);

        final Network cgm = displacePisaNodes(task.getArtifact(TOPOLOGICAL_MERGE_FILE, Network.class));
        saveArtifactFileWithWriter(CGM_FILE_AFTER_PISA_RENAMING, task, configuration, path -> cgm.write(UCTE_FORMAT, properties, path));
    }

    private static Network displacePisaNodes(final Network cgm) {
        prepareFrenchBoundary(cgm);
        moveItalianBoundary(cgm);
        return cgm;
    }

    private static void prepareFrenchBoundary(final Network cgm) {
        final List<Line> linesToRemove = new ArrayList<>(cgm.getLineStream()
                                                                 .filter(PisaService::wouldBecomeLineBetweenXnodes)
                                                                 .toList());

        final List<BoundaryLine> boundaryLinesToRemove = cgm.getBoundaryLineStream()
                .filter(PisaService::wouldBecomeLineBetweenXnodes)
                .toList();

        Stream.concat(linesToRemove.stream(), boundaryLinesToRemove.stream())
                .forEach(PisaService::tryToRemove);
    }

    public static void fixPisaNetPositions(final MergingTask task, final NetPositionsResults netPositionsFile) {
        final NetPositions italyNetPosition = netPositionsFile.get(IT);
        final NetPositions franceNetPosition = netPositionsFile.get(FR);

        if (anyNull(italyNetPosition, franceNetPosition)) {
            return;
        }

        //  since XPI will no longer be XNodes, we remove IPIOTA11->IPI = IPIOTA11->XPI from italian NP
        CURRENT_TO_FUTURE_ITA_HUBS.forEach((xpi, xfg) -> {
            italyNetPosition.removeVirtualHubFlow(getPisaBoundaryP0(xpi, IT, task));
            italyNetPosition.getVirtualHubsExchanges().put(xfg, -getPisaBoundaryP0(xpi, FR, task));
            italyNetPosition.getVirtualHubsExchanges().remove(xpi);
        });

        // since FFG will become XNodes, we add IPI->XFG(ITA) to italian NP
        //                                      = -XFG(ITA)->IPI = FFG(FR)->XPI
        CURRENT_TO_FUTURE_FR_HUBS.forEach((xpi, xfg) -> {
            final double ffgToXpi = getPisaBoundaryP0(xpi, FR, task);
            italyNetPosition.addVirtualHubFlow(ffgToXpi);
            franceNetPosition.getVirtualHubsExchanges().put(xfg, ffgToXpi);
            franceNetPosition.getVirtualHubsExchanges().remove(xpi);
        });

        // for french NP we remove FFG(FR)->XPI and add FG.ILE->XFG(FR) = -IPI->XFG(ITA) = FFG(FR)->XPI
        // <=> it remains unchanged

    }

    private static double getPisaBoundaryP0(final String pisaNode, final Country country, final MergingTask task) {
        return task.getPreTreatedOrInputIgm(country).getBoundaryLineStream()
                .filter(isPairedWith(pisaNode).and(isInCountry(country)))
                .findFirst()
                .map(BoundaryLine::getP0)
                .orElse(0.);
    }

    private static void tryToRemove(final Connectable<?> connectable) {
        if (isInOutage(connectable)) {
            connectable.remove();
        } else {
            throw new CeMergingException("PiSa post-treatment: cannot remove line %s because it is in operation."
                                                 .formatted(connectable.getNameOrId()));
        }
    }

    private static void moveItalianBoundary(final Network cgm) {
        final List<BoundaryLine> pisaItalySide = cgm.getBoundaryLineStream()
                .filter(bl -> willBecomeItalianNode(bl.getPairingKey()))
                .toList();

        PisaXnodesReplacer.on(cgm).replace(pisaItalySide);
    }

    private static boolean wouldBecomeLineBetweenXnodes(final Line line) {
        final Terminal terminal1 = line.getTerminal1();
        final Terminal terminal2 = line.getTerminal2();

        return willBecomeFrenchXnode(terminal1) && willBecomeFrenchXnode(terminal2);
    }

    private static boolean wouldBecomeLineBetweenXnodes(final BoundaryLine line) {
        final String node = line.getPairingKey();
        final Terminal terminal = line.getTerminal();

        return !willBecomeItalianNode(node) && willBecomeFrenchXnode(terminal);
    }

    public static boolean willBecomeItalianNode(final String node) {
        return XPI_NODE_NAME_PATTERN.matcher(node).matches();
    }

    public static boolean willBecomeFrenchXnode(final Terminal terminal) {
        return willBecomeFrenchXnode(getNodeName(terminal));
    }

    private static boolean willBecomeFrenchXnode(final String nodeName) {
        return FFG_NODE_NAME_PATTERN.matcher(nodeName).matches();
    }

}

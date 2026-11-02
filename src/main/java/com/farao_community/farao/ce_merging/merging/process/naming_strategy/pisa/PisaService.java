/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PISA_NAMING_STRATEGY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_EXPORT_NAMING_STRATEGY_PROPERTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.save;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getNodeName;
import static com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa.PisaNamingStrategy.FFG_NODE_NAME_PATTERN;
import static com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa.PisaNamingStrategy.XPI_NODE_NAME_PATTERN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_COMPENSATION;

@Service
public class PisaService {
    private final CeMergingConfiguration configuration;

    public PisaService(final CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void displacePisaNodes(final MergingTask task) throws FileNotFoundException {
        final Properties properties = new Properties();
        properties.setProperty(UCTE_EXPORT_NAMING_STRATEGY_PROPERTY, PISA_NAMING_STRATEGY);

        final Network cgm = displacePisaNodes(task.getArtifact(CGM_FILE_AFTER_COMPENSATION, Network.class));

        final SavedFile cgmFile = save(configuration.getOutputsDirectoryPath(task),
                                       task.getOutputCgmFileName(),
                                       String.format("/tasks/%d/outputs/cgm", task.getId()),
                                       path -> cgm.write(UCTE_FORMAT, properties, path));

        task.getOutputs().setCgm(cgmFile);
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

    private static void tryToRemove(final Connectable connectable) {
        if (NetworkUtil.isInOutage(connectable)) {
            connectable.remove();
        } else {
            throw new CeMergingException("PiSa post-treatment: cannot remove line %s because it is in operation."
                                                 .formatted(connectable.getNameOrId()));
        }
    }

    private static void moveItalianBoundary(final Network cgm) {
        final List<BoundaryLine> pisaItalySide = cgm.getBoundaryLineStream()
                .filter(PisaService::willHaveItalianNode)
                .toList();

        PisaXnodesReplacer.on(cgm).replace(pisaItalySide);
    }

    private static boolean willHaveItalianNode(final BoundaryLine boundaryLine) {
        return willBecomeItalianNode(boundaryLine.getPairingKey());
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

    private static boolean willBecomeItalianNode(final String node) {
        return XPI_NODE_NAME_PATTERN.matcher(node).matches();
    }

    public static boolean willBecomeFrenchXnode(final Terminal terminal) {
        return willBecomeFrenchXnode(getNodeName(terminal));
    }

    private static boolean willBecomeFrenchXnode(final String nodeName) {
        return FFG_NODE_NAME_PATTERN.matcher(nodeName).matches();
    }

}

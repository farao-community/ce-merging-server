/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.hvdc_alignment;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.network.UcteElementStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HvdcXNodeAlignmentTest {

    private static final String RESOURCE_PATH = "/hvdc_alignment/";
    private static final String DK_X_NODE = "XXX_EE1D";

    private final VirtualHubsAlignmentCouple alignmentCouples = new VirtualHubsAlignmentCouple("XXX_EE1N", DK_X_NODE);

    @Test
    void shouldAlignRecessiveNodesWhenNlInOutage() {
        final Network network = readNetwork("nl_outage.uct");
        HvdcXNodeAlignment.on(network, network, alignmentCouples).align();
        assertBoundaryLine(network, DK_X_NODE, 0., 0., 0., 0., UcteElementStatus.BUSBAR_COUPLER_OUT_OF_OPERATION
        );
    }

    @Test
    void shouldAlignRecessiveNodesWhenDkInOutage() {
        final Network network = readNetwork("dk_outage.uct");
        HvdcXNodeAlignment.on(network, network, alignmentCouples).align();
        assertBoundaryLine(network, DK_X_NODE, 106., 0., 3., 0., UcteElementStatus.EQUIVALENT_ELEMENT_IN_OPERATION);
    }

    @Test
    void shouldAlignRecessiveNodesWhenNoOutage() {
        final Network network = readNetwork("no_outage.uct");
        HvdcXNodeAlignment.on(network, network, alignmentCouples).align();
        assertBoundaryLine(network, DK_X_NODE, -4., 11.1000, 6., 0.00324, UcteElementStatus.REAL_ELEMENT_IN_OPERATION);
    }

    private static Network readNetwork(final String filename) {
        return Network.read(
                HvdcXNodeAlignmentTest.class
                        .getResource(RESOURCE_PATH + filename)
                        .getPath()
        );
    }

    private static void assertBoundaryLine(final Network network,
                                           final String nodeName,
                                           final double expectedP0,
                                           final double expectedQ0,
                                           final double expectedTargetP,
                                           final double expectedTargetQ,
                                           final UcteElementStatus expectedStatus) {
        final BoundaryLine boundaryLine = network.getBoundaryLineStream()
                .filter(bl -> bl.getPairingKey().equals(nodeName))
                .findFirst()
                .orElseThrow();
        assertEquals(expectedP0, boundaryLine.getP0(), 0.01);
        assertEquals(expectedQ0, boundaryLine.getQ0(), 0.01);
        assertEquals(expectedTargetP, boundaryLine.getGeneration().getTargetP(), 0.01);
        assertEquals(expectedTargetQ, boundaryLine.getGeneration().getTargetQ(), 0.01);
        assertEquals(expectedStatus, HvdcXNodeAlignment.getStatus(boundaryLine));
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.Xnode;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XnodesCalculationTest {
    private static final String XNODE_NAME = "XNODE1";
    private static final String UNKNOWN_XNODE = "UNKNOWN";
    private static final String NETWORK_NAME = "network";

    private XnodesCalculation calculation;

    @BeforeEach
    void setUp() {
        calculation = new XnodesCalculation();
    }

    //TODO Add TU with fake file

    @Test
    void checkXnodesConfigConsistencyTest() {
        final Network network = mock(Network.class);
        final DanglingLine dl = mock(DanglingLine.class);
        final Xnode xnode = mock(Xnode.class);
        when(dl.getPairingKey()).thenReturn(XNODE_NAME);
        when(network.getDanglingLineStream()).thenReturn(Stream.of(dl));
        when(network.getNameOrId()).thenReturn(NETWORK_NAME);
        when(xnode.getName()).thenReturn(XNODE_NAME);

        assertDoesNotThrow(() -> calculation.checkXnodesConfigConsistency(network, Collections.emptyList(), List.of(xnode)));
    }

    @Test
    void shouldThrowWhenXnodeIsMissing() {
        final Network network = mock(Network.class);
        final DanglingLine dl = mock(DanglingLine.class);

        when(dl.getPairingKey()).thenReturn(UNKNOWN_XNODE);
        when(network.getDanglingLineStream()).thenReturn(Stream.of(dl));
        when(network.getNameOrId()).thenReturn(NETWORK_NAME);

        assertThrows(CeMergingException.class, () -> calculation.checkXnodesConfigConsistency(network, Collections.emptyList(), Collections.emptyList()));
    }
}

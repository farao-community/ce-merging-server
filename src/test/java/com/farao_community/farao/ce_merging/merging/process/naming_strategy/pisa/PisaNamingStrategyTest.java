/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Switch;
import com.powsybl.ucte.converter.UcteException;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PISA_NAMING_STRATEGY;
import static com.powsybl.ucte.network.UcteCountryCode.XX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PisaNamingStrategyTest {

    private PisaNamingStrategy namingStrategy;

    @BeforeEach
    void setUp() {
        namingStrategy = new PisaNamingStrategy();
    }

    @Test
    void shouldReturnNamingStrategyName() {
        assertEquals(PISA_NAMING_STRATEGY, namingStrategy.getName());
    }

    @Test
    void shouldInitializeNetworkWithoutError() {
        final Network network = mock(Network.class);
        assertDoesNotThrow(() -> namingStrategy.initializeNetwork(network));
    }

    @Test
    void shouldConvertFfgNode() {
        final UcteNodeCode nodeCode = namingStrategy.getUcteNodeCode("FFG.IL11");
        assertEquals(XX, nodeCode.getUcteCountryCode());
        assertEquals('0', nodeCode.getBusbar());
    }

    @Test
    void shouldConvertAllFfgNodes() {
        for (int i = 11; i <= 14; i++) {
            UcteNodeCode nodeCode = namingStrategy.getUcteNodeCode("FFG.IL" + i);
            assertEquals(XX, nodeCode.getUcteCountryCode());
            assertEquals('0', nodeCode.getBusbar());
        }
    }

    @Test
    void shouldConvertNodeCodeFromBus() {
        final Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("FFG.IL11");
        final UcteNodeCode nodeCode = namingStrategy.getUcteNodeCode(bus);
        assertEquals(XX, nodeCode.getUcteCountryCode());
        assertEquals('0', nodeCode.getBusbar());
    }

    @Test
    void shouldThrowExceptionForInvalidNodeCode() {
        final UcteException exception = assertThrows(
                UcteException.class,
                () -> namingStrategy.getUcteNodeCode("INVALID")
        );

        assertTrue(exception.getMessage().contains("Invalid UCTE node identifier: INVALID"));
    }

    @Test
    void shouldConvertElementId() {
        final UcteElementId elementId = namingStrategy.getUcteElementId("FFG.IL11 FFG.IL12 1");
        assertNotNull(elementId);
        assertEquals(XX, elementId.getNodeCode1().getUcteCountryCode());
        assertEquals('0', elementId.getNodeCode1().getBusbar());
        assertEquals(XX, elementId.getNodeCode2().getUcteCountryCode());
        assertEquals('0', elementId.getNodeCode2().getBusbar());
    }

    @Test
    void shouldConvertElementIdFromSwitch() {
        final Switch sw = mock(Switch.class);
        when(sw.getId()).thenReturn("FFG.IL11 FFG.IL12 1");
        final UcteElementId elementId = namingStrategy.getUcteElementId(sw);
        assertEquals(XX, elementId.getNodeCode1().getUcteCountryCode());
        assertEquals(XX, elementId.getNodeCode2().getUcteCountryCode());
    }

    @Test
    void shouldConvertElementIdFromBranch() {
        final Branch<?> branch = mock(Branch.class);
        when(branch.getId()).thenReturn("FFG.IL11 FFG.IL12 1");
        final UcteElementId elementId = namingStrategy.getUcteElementId(branch);
        assertEquals(XX, elementId.getNodeCode1().getUcteCountryCode());
        assertEquals(XX, elementId.getNodeCode2().getUcteCountryCode());
    }

    @Test
    void shouldConvertElementIdFromDanglingLine() {
        final BoundaryLine boundaryLine = mock(BoundaryLine.class);
        when(boundaryLine.getId()).thenReturn("FFG.IL11 FFG.IL12 1");
        final UcteElementId elementId = namingStrategy.getUcteElementId(boundaryLine);
        assertEquals(XX, elementId.getNodeCode1().getUcteCountryCode());
        assertEquals(XX, elementId.getNodeCode2().getUcteCountryCode());
    }

    @Test
    void shouldThrowExceptionForInvalidElementId() {
        final UcteException exception = assertThrows(
                UcteException.class,
                () -> namingStrategy.getUcteElementId("INVALID")
        );
        assertTrue(exception.getMessage().contains("Invalid UCTE element identifier: INVALID"));
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.monita;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.network.UcteElementId;
import org.junit.jupiter.api.Test;

import static com.powsybl.ucte.network.UcteCountryCode.IT;
import static com.powsybl.ucte.network.UcteCountryCode.XX;
import static com.powsybl.ucte.network.UcteVoltageLevelCode.VL_220;
import static com.powsybl.ucte.network.UcteVoltageLevelCode.VL_380;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.assertions.UcteNodeCodeAssert.assertThat;

class MonitaNamingStrategyTest {
    private NamingStrategy strategy = new MonitaNamingStrategy();

    @Test
    void getName() {
        assertEquals("MonitaNamingStrategy", strategy.getName());
    }

    @Test
    void getUcteNodeCodeWithMonitaId() {
        assertThat(strategy.getUcteNodeCode("IKOTR121"))
            .isLocatedIn(XX, "KOTR1")
            .isBusBar(VL_220, '0');
    }

    @Test
    void getUcteNodeCodeWithAnotherId() {
        assertThat(strategy.getUcteNodeCode("I190V121"))
            .isLocatedIn(IT, "190V1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteNodeCodeWithMonitaBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("IKOTR121");

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(XX, "KOTR1")
            .isBusBar(VL_220, '0');
    }

    @Test
    void getUcteNodeCodeWithAnotherBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("I190V121");

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(IT, "190V1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteNodeWithMonitaDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XKO_LA11");

        assertThat(strategy.getUcteNodeCode(danglingLine))
            .isLocatedIn(XX, "KO_LA")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteNodeWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XAL_PO21");

        assertThat(strategy.getUcteNodeCode(danglingLine))
            .isLocatedIn(XX, "AL_PO")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("IKOTR121 IKOTR111 1");

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "KOTR1")
            .isBusBar(VL_220, '0');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "KOTR1")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("I190V121 ITVSV121 1");

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(IT, "190V1")
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "TVSV1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("IKOTR121 IKOTR111 1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "KOTR1")
            .isBusBar(VL_220, '0');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "KOTR1")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherBranch() {
        Branch<?> branch = mock(Branch.class);
        when(branch.getId()).thenReturn("I190V121 ITVSV121 1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(IT, "190V1")
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "TVSV1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XKO_LA11 IKOTR111 1");

        UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "KO_LA")
            .isBusBar(VL_380, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "KOTR1")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XAL_PO21 IPONTA21 1");

        UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "AL_PO")
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "PONTA")
            .isBusBar(VL_220, '1');
    }
}

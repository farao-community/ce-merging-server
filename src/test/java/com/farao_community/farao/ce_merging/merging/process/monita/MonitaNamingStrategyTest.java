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
    private final NamingStrategy strategy = new MonitaNamingStrategy();
    private static final String ITALY_MONITA_CODE = "IKOTR121";
    private static final String ITALY_OTHER_CODE = "I190V121";

    private static final String ITALY_MONITA_SPOT = "KOTR1";
    private static final String ITALY_OTHER_SPOT = "190V1";

    private static final String MONITA_BRANCH = "IKOTR121 IKOTR111 1";
    private static final String ITALY_OTHER_BRANCH = "I190V121 ITVSV121 1";

    @Test
    void getName() {
        assertEquals("MonitaNamingStrategy", strategy.getName());
    }

    @Test
    void getUcteNodeCodeWithMonitaId() {
        assertThat(strategy.getUcteNodeCode(ITALY_MONITA_CODE))
            .isLocatedIn(XX, ITALY_MONITA_SPOT)
            .isBusBar(VL_220, '0');
    }

    @Test
    void getUcteNodeCodeWithAnotherId() {
        assertThat(strategy.getUcteNodeCode(ITALY_OTHER_CODE))
            .isLocatedIn(IT, ITALY_OTHER_SPOT)
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteNodeCodeWithMonitaBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn(ITALY_MONITA_CODE);

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(XX, ITALY_MONITA_SPOT)
            .isBusBar(VL_220, '0');
    }

    @Test
    void getUcteNodeCodeWithAnotherBus() {
        final Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn(ITALY_OTHER_CODE);

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(IT, ITALY_OTHER_SPOT)
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteNodeWithMonitaDanglingLine() {
        final DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XKO_LA11");

        assertThat(strategy.getUcteNodeCode(danglingLine))
            .isLocatedIn(XX, "KO_LA")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteNodeWithAnotherDanglingLine() {
        final DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XAL_PO21");

        assertThat(strategy.getUcteNodeCode(danglingLine))
            .isLocatedIn(XX, "AL_PO")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaId() {
        final UcteElementId ucteElementId = strategy.getUcteElementId(MONITA_BRANCH);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, ITALY_MONITA_SPOT)
            .isBusBar(VL_220, '0');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, ITALY_MONITA_SPOT)
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherId() {
        final UcteElementId ucteElementId = strategy.getUcteElementId(ITALY_OTHER_BRANCH);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(IT, ITALY_OTHER_SPOT)
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "TVSV1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaBranch() {
        final Branch<?> branch = mock(Branch.class);
        when(branch.getId()).thenReturn(MONITA_BRANCH);

        final UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, ITALY_MONITA_SPOT)
            .isBusBar(VL_220, '0');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, ITALY_MONITA_SPOT)
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherBranch() {
        final Branch<?> branch = mock(Branch.class);
        when(branch.getId()).thenReturn(ITALY_OTHER_BRANCH);

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(IT, ITALY_OTHER_SPOT)
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "TVSV1")
            .isBusBar(VL_220, '1');
    }

    @Test
    void getUcteElementIdWithMonitaDanglingLine() {
        final DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XKO_LA11 IKOTR111 1");

        final UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "KO_LA")
            .isBusBar(VL_380, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, ITALY_MONITA_SPOT)
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithAnotherDanglingLine() {
        final DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XAL_PO21 IPONTA21 1");

        final UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "AL_PO")
            .isBusBar(VL_220, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(IT, "PONTA")
            .isBusBar(VL_220, '1');
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.dk_renaming;

import com.farao_community.farao.ce_merging.common.CeMergingConstants;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.network.UcteElementId;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.powsybl.ucte.network.UcteCountryCode.DK;
import static com.powsybl.ucte.network.UcteCountryCode.XX;
import static com.powsybl.ucte.network.UcteVoltageLevelCode.VL_150;
import static com.powsybl.ucte.network.UcteVoltageLevelCode.VL_380;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.assertions.UcteNodeCodeAssert.assertThat;

public class DKNamingStrategyTest {
    private static final String DK_HVDC_XNODES = "FG_HK,TJ_K1,TJ_K2,TJ_K3,TJ_K4,VH_L1,VH_L2,BI_R1,BI_R2";

    private static NamingStrategy strategy;

    @BeforeAll
    static void beforeAll() {
        Network network = mock(Network.class);
        when(network.getProperty(CeMergingConstants.DK_HVDC_XNODES_PROPERTY)).thenReturn(DK_HVDC_XNODES);
        strategy = new DKNamingStrategy();
        strategy.initializeNetwork(network);
    }

    @Test
    void getName() {
        assertEquals("DKNamingStrategy", strategy.getName());
    }

    @Test
    void getUcteNodeCodeWithDkHvdcId() {
        assertThat(strategy.getUcteNodeCode("DBI_R11K"))
            .isLocatedIn(XX, "BI_R1")
            .isBusBar(VL_380, 'K');
    }

    @Test
    void getUcteNodeCodeWithAnotherId() {
        assertThat(strategy.getUcteNodeCode("D1ABS_31"))
            .isLocatedIn(DK, "1ABS_")
            .isBusBar(VL_150, '1');
    }

    @Test
    void getUcteNodeCodeWithDkHvdcBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("DBI_R11K");

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(XX, "BI_R1")
            .isBusBar(VL_380, 'K');
    }

    @Test
    void getUcteNodeCodeWithAnotherBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("D1ABS_31");

        assertThat(strategy.getUcteNodeCode(bus))
            .isLocatedIn(DK, "1ABS_")
            .isBusBar(VL_150, '1');
    }

    @Test
    void getUcteNodeCodeWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XFL_KA11");

        assertThat(strategy.getUcteNodeCode(danglingLine))
            .isLocatedIn(XX, "FL_KA")
            .isBusBar(VL_380, '1');
    }

    @Test
    void getUcteElementIdWithDkHvdcId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("DBI_R11K D1REV_1  1");

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "BI_R1")
            .isBusBar(VL_380, 'K');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(DK, "1REV_")
            .isBusBar(VL_380, ' ');
    }

    @Test
    void getUcteElementIdWithAnotherId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("D1ABS_31 D1SQN_3  1");

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(DK, "1ABS_")
            .isBusBar(VL_150, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(DK, "1SQN_")
            .isBusBar(VL_150, ' ');
    }

    @Test
    void getUcteElementIdWithDkHvdcBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("DBI_R11K D1REV_1  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "BI_R1")
            .isBusBar(VL_380, 'K');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(DK, "1REV_")
            .isBusBar(VL_380, ' ');
    }

    @Test
    void getUcteElementIdWithAnotherBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("D1ABS_31 D1SQN_3  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(DK, "1ABS_")
            .isBusBar(VL_150, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(DK, "1SQN_")
            .isBusBar(VL_150, ' ');
    }

    @Test
    void getUcteElementIdWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XFL_KA11 D1KAS_1  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertThat(ucteElementId.getNodeCode1())
            .isLocatedIn(XX, "FL_KA")
            .isBusBar(VL_380, '1');

        assertThat(ucteElementId.getNodeCode2())
            .isLocatedIn(DK, "1KAS_")
            .isBusBar(VL_380, ' ');
    }
}

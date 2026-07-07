/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy;

import com.farao_community.farao.ce_merging.common.CeMergingConstants;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.converter.NamingStrategy;
import com.powsybl.ucte.network.UcteCountryCode;
import com.powsybl.ucte.network.UcteElementId;
import com.powsybl.ucte.network.UcteNodeCode;
import com.powsybl.ucte.network.UcteVoltageLevelCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        UcteNodeCode ucteNodeCode = strategy.getUcteNodeCode("DBI_R11K");

        assertEquals(UcteCountryCode.XX, ucteNodeCode.getUcteCountryCode());
        assertEquals("BI_R1", ucteNodeCode.getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteNodeCode.getVoltageLevelCode());
        assertEquals('K', ucteNodeCode.getBusbar());
    }

    @Test
    void getUcteNodeCodeWithAnotherId() {
        UcteNodeCode ucteNodeCode = strategy.getUcteNodeCode("D1ABS_31");

        assertEquals(UcteCountryCode.DK, ucteNodeCode.getUcteCountryCode());
        assertEquals("1ABS_", ucteNodeCode.getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteNodeCode.getVoltageLevelCode());
        assertEquals('1', ucteNodeCode.getBusbar());
    }

    @Test
    void getUcteNodeCodeWithDkHvdcBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("DBI_R11K");

        UcteNodeCode ucteNodeCode = strategy.getUcteNodeCode(bus);

        assertEquals(UcteCountryCode.XX, ucteNodeCode.getUcteCountryCode());
        assertEquals("BI_R1", ucteNodeCode.getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteNodeCode.getVoltageLevelCode());
        assertEquals('K', ucteNodeCode.getBusbar());
    }

    @Test
    void getUcteNodeCodeWithAnotherBus() {
        Bus bus = mock(Bus.class);
        when(bus.getId()).thenReturn("D1ABS_31");

        UcteNodeCode ucteNodeCode = strategy.getUcteNodeCode(bus);

        assertEquals(UcteCountryCode.DK, ucteNodeCode.getUcteCountryCode());
        assertEquals("1ABS_", ucteNodeCode.getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteNodeCode.getVoltageLevelCode());
        assertEquals('1', ucteNodeCode.getBusbar());
    }

    @Test
    void getUcteNodeCodeWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn("XFL_KA11");

        UcteNodeCode ucteNodeCode = strategy.getUcteNodeCode(danglingLine);

        assertEquals(UcteCountryCode.XX, ucteNodeCode.getUcteCountryCode());
        assertEquals("FL_KA", ucteNodeCode.getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteNodeCode.getVoltageLevelCode());
        assertEquals('1', ucteNodeCode.getBusbar());
    }

    @Test
    void getUcteElementIdWithDkHvdcId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("DBI_R11K D1REV_1  1");

        assertEquals(UcteCountryCode.XX, ucteElementId.getNodeCode1().getUcteCountryCode());
        assertEquals("BI_R1", ucteElementId.getNodeCode1().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode1().getVoltageLevelCode());
        assertEquals('K', ucteElementId.getNodeCode1().getBusbar());

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode2().getUcteCountryCode());
        assertEquals("1REV_", ucteElementId.getNodeCode2().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode2().getVoltageLevelCode());
        assertEquals(' ', ucteElementId.getNodeCode2().getBusbar());
    }

    @Test
    void getUcteElementIdWithAnotherId() {
        UcteElementId ucteElementId = strategy.getUcteElementId("D1ABS_31 D1SQN_3  1");

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode1().getUcteCountryCode());
        assertEquals("1ABS_", ucteElementId.getNodeCode1().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteElementId.getNodeCode1().getVoltageLevelCode());
        assertEquals('1', ucteElementId.getNodeCode1().getBusbar());

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode2().getUcteCountryCode());
        assertEquals("1SQN_", ucteElementId.getNodeCode2().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteElementId.getNodeCode2().getVoltageLevelCode());
        assertEquals(' ', ucteElementId.getNodeCode2().getBusbar());
    }

    @Test
    void getUcteElementIdWithDkHvdcBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("DBI_R11K D1REV_1  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertEquals(UcteCountryCode.XX, ucteElementId.getNodeCode1().getUcteCountryCode());
        assertEquals("BI_R1", ucteElementId.getNodeCode1().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode1().getVoltageLevelCode());
        assertEquals('K', ucteElementId.getNodeCode1().getBusbar());

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode2().getUcteCountryCode());
        assertEquals("1REV_", ucteElementId.getNodeCode2().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode2().getVoltageLevelCode());
        assertEquals(' ', ucteElementId.getNodeCode2().getBusbar());
    }

    @Test
    void getUcteElementIdWithAnotherBranch() {
        Branch branch = mock(Branch.class);
        when(branch.getId()).thenReturn("D1ABS_31 D1SQN_3  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(branch);

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode1().getUcteCountryCode());
        assertEquals("1ABS_", ucteElementId.getNodeCode1().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteElementId.getNodeCode1().getVoltageLevelCode());
        assertEquals('1', ucteElementId.getNodeCode1().getBusbar());

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode2().getUcteCountryCode());
        assertEquals("1SQN_", ucteElementId.getNodeCode2().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_150, ucteElementId.getNodeCode2().getVoltageLevelCode());
        assertEquals(' ', ucteElementId.getNodeCode2().getBusbar());
    }

    @Test
    void getUcteElementIdWithAnotherDanglingLine() {
        DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getId()).thenReturn("XFL_KA11 D1KAS_1  1");

        UcteElementId ucteElementId = strategy.getUcteElementId(danglingLine);

        assertEquals(UcteCountryCode.XX, ucteElementId.getNodeCode1().getUcteCountryCode());
        assertEquals("FL_KA", ucteElementId.getNodeCode1().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode1().getVoltageLevelCode());
        assertEquals('1', ucteElementId.getNodeCode1().getBusbar());

        assertEquals(UcteCountryCode.DK, ucteElementId.getNodeCode2().getUcteCountryCode());
        assertEquals("1KAS_", ucteElementId.getNodeCode2().getGeographicalSpot());
        assertEquals(UcteVoltageLevelCode.VL_380, ucteElementId.getNodeCode2().getVoltageLevelCode());
        assertEquals(' ', ucteElementId.getNodeCode2().getBusbar());
    }
}

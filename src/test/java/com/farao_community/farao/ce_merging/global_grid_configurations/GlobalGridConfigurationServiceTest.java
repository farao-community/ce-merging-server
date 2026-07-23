/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.util.CountryUtils.KOSOVO_CODE;
import static com.farao_community.farao.ce_merging.common.util.CountryUtils.KOSOVO_ISO_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;

@SpringBootTest
class GlobalGridConfigurationServiceTest {
    @Autowired
    GlobalGridConfigurationService service;
    private MergingTask task;

    @BeforeEach
    void setUp() {
        task = new MergingTask();
        final Inputs inputs = new Inputs();
        inputs.setTargetDate(BEGINNING_OF_2000);
        task.setInputs(inputs);
        TaskTestUtils.setTaskDefaultConfigurations(task);
    }

    @Test
    void taskShouldContainDefaultVirtualHubsConfigurations() throws IOException {
        service.setVirtualHubsConfiguration(task);

        List<VirtualHubRecord> defaultVirtualHubs = task.getConfigurations().getVirtualHubList();
        List<BorderDirectionRecord> defaultBorderDirection = task.getConfigurations().getBorderDirectionRecords();

        assertNotNull(defaultVirtualHubs);
        assertEquals(35, defaultVirtualHubs.size());
        // sort virtualHubs alphabetically based on code
        defaultVirtualHubs.sort(Comparator.comparing(VirtualHubRecord::getCode));

        VirtualHubRecord beAlegroVirtualHub = defaultVirtualHubs.get(0);
        assertEquals("BE_AL", beAlegroVirtualHub.getCode());
        assertEquals("22Y201903144---9", beAlegroVirtualHub.getEic());
        assertEquals("XLI_OB1B", beAlegroVirtualHub.getNodeName());

        VirtualHubRecord deAlegroVirtualHub = defaultVirtualHubs.get(1);
        assertEquals("DE_AL", deAlegroVirtualHub.getCode());
        assertEquals("22Y201903145---4", deAlegroVirtualHub.getEic());
        assertEquals("XLI_OB1A", deAlegroVirtualHub.getNodeName());

        VirtualHubRecord dk1CobraVirtualHub = defaultVirtualHubs.get(2);
        assertEquals("DK1_COBRA", dk1CobraVirtualHub.getCode());
        assertEquals("10Y1001C--00001J", dk1CobraVirtualHub.getEic());
        assertEquals("XED_EE1N", dk1CobraVirtualHub.getNodeName());

        List<BorderDirectionRecord> borderDirectionRecordWithKs = getBorderDirectionRecordsByCountry(defaultBorderDirection, KOSOVO_CODE);
        assertEquals(0, borderDirectionRecordWithKs.size());

        List<BorderDirectionRecord> borderDirectionRecordWithXK = getBorderDirectionRecordsByCountry(defaultBorderDirection, KOSOVO_ISO_CODE);
        assertEquals(4, borderDirectionRecordWithXK.size());
    }

    @Test
    void taskShouldContainDefaultXnodesConfigurations() throws IOException {
        service.setXnodesConfiguration(task);

        List<XnodeConfig> xnodes = task.getConfigurations().getXnodeList();
        assertNotNull(xnodes);
        assertEquals(458, xnodes.size());

        XnodeConfig xnode = xnodes.get(0);
        assertEquals("XBI_MO31", xnode.getName());
        assertEquals("AL", xnode.getArea1());
        assertNull(xnode.getSubarea1());
        assertEquals("GR", xnode.getArea2());
        assertNull(xnode.getSubarea2());
    }

    @Test
    void taskShouldContainDefaultHvdcAlignmentConfigurations() throws IOException {
        service.setHvdcXNodeAlignmentConfiguration(task);

        VirtualHubsAlignmentCouple xedCouple = new VirtualHubsAlignmentCouple("XED_EE1N", "XED_EE1D");
        VirtualHubsAlignmentCouple xarCouple = new VirtualHubsAlignmentCouple("XAR_GA1I", "XAR_GA1G");
        final List<VirtualHubsAlignmentCouple> expectedVirtualHubsAlignmentCouples = List.of(xedCouple, xarCouple);

        ZeroFlowNode xbaKf31Node = new ZeroFlowNode("XBA_KF31", "DE");
        ZeroFlowNode xbaKf32Node = new ZeroFlowNode("XBA_KF32", "DE");
        final List<ZeroFlowNode> expectedZeroFlowNodes = List.of(xbaKf31Node, xbaKf32Node);

        final List<String> expectedDkHvdcXnodes = List.of("FG_HK", "TJ_K1", "TJ_K2", "TJ_K3", "TJ_K4", "VH_L1", "VH_L2", "BI_R1", "BI_R2");
        final String expectedDefaultSlackNode = "ELA MU11";

        Configurations configurations = task.getConfigurations();
        List<VirtualHubsAlignmentCouple> virtualHubsAlignmentCouples = configurations.getVirtualHubsAlignmentCouples();
        List<ZeroFlowNode> zeroFlowNodes = configurations.getZeroFlowNodes();
        List<String> dkHvdcXnodes = configurations.getDkHvdcXnodes();
        String defaultSlackNode = configurations.getDefaultSlackNode();

        assertEquals(expectedVirtualHubsAlignmentCouples, virtualHubsAlignmentCouples);
        assertEquals(expectedZeroFlowNodes, zeroFlowNodes);
        assertEquals(expectedDkHvdcXnodes, dkHvdcXnodes);
        assertEquals(expectedDefaultSlackNode, defaultSlackNode);
    }

    @Test
    void taskShouldContainDefaultRegionConfigurations() throws IOException {
        service.setRegionEicConfiguration(task);

        final String expectedNameConfig = "CORE";

        final Map<String, String> expectedAreasIn = Map.ofEntries(
            Map.entry("BE", "10YBE----------2"),
            Map.entry("SK", "10YSK-SEPS-----K"),
            Map.entry("DE", "10YCB-GERMANY--8"),
            Map.entry("HU", "10YHU-MAVIR----U"),
            Map.entry("NL", "10YNL----------L"),
            Map.entry("AT", "10YAT-APG------L"),
            Map.entry("CZ", "10YCZ-CEPS-----N"),
            Map.entry("HR", "10YHR-HEP------M"),
            Map.entry("PL", "10YPL-AREA-----S"),
            Map.entry("RO", "10YRO-TEL------P"),
            Map.entry("SI", "10YSI-ELES-----O"),
            Map.entry("FR", "10YFR-RTE------C")
        );

        final Map<String, String> expectedAreasOut = Map.ofEntries(
            Map.entry("AL", "10YCB-ALBANIA--1"),
            Map.entry("BA", "10YBA-JPCC-----D"),
            Map.entry("BG", "10YCA-BULGARIA-R"),
            Map.entry("CH", "10YCB-SWITZERL-D"),
            Map.entry("DK", "10YDK-1--------W"),
            Map.entry("ES", "10YES-REE------0"),
            Map.entry("GR", "10YCB-GREECE---2"),
            Map.entry("IT", "10YIT-GRTN-----B"),
            Map.entry("ME", "10YCS-CG-TSO---S"),
            Map.entry("MK", "10YMK-MEPSO----8"),
            Map.entry("PT", "10YPT-REN------W"),
            Map.entry("RS", "10YCS-SERBIATSOV"),
            Map.entry("TR", "10YCB-TURKEY---V"),
            Map.entry("UA", "10Y1001C--00003F"),
            Map.entry("XK", "10Y1001C--00100H"),
            Map.entry("GB", "10YGB----------A"),
            Map.entry("SE", "10YSE-1--------K"),
            Map.entry("IS", "10Y1001A1001A958"),
            Map.entry("CY", "10YCY-1001A0003J"),
            Map.entry("MA", "10YMA-ONE------O"),
            Map.entry("LT", "10YLT-1001A0008Q"),
            Map.entry("NO", "10YNO-0--------C"),
            Map.entry("MD", "10Y1001A1001A990")
        );

        final Map<String, TsoInfos> expectedGermanyZone = Map.ofEntries(
            Map.entry("D2", createTsoInfos("TenneT DE", "10XDE-EON-NETZ-C")),
            Map.entry("D4", createTsoInfos("TransnetBW", "11XENBW-N------E")),
            Map.entry("D7", createTsoInfos("Amprion", "10XDE-RWENET---W")),
            Map.entry("D8", createTsoInfos("50Hertz", "10XDE-VE-TRANSMK")),
            Map.entry("D6", createTsoInfos("Creos", "21X000000001333E"))
        );

        final Map<String, String> expectedAreasAll = new HashMap<>();
        expectedAreasAll.putAll(expectedAreasIn);
        expectedAreasAll.putAll(expectedAreasOut);

        RegionConfiguration regionConfiguration = task.getConfigurations().getRegionConfiguration();
        String nameConfig = regionConfiguration.getName();
        Map<String, String> areasIn = regionConfiguration.getAreasIn();
        Map<String, String> areasOut = regionConfiguration.getAreasOut();
        Map<String, String> areasAll = regionConfiguration.getAreasAll();
        Map<String, TsoInfos> germanyZone = regionConfiguration.getGermanyZone();

        assertEquals(expectedNameConfig, nameConfig);
        assertEquals(expectedAreasIn, areasIn);
        assertEquals(expectedAreasOut, areasOut);
        assertEquals(expectedAreasAll, areasAll);
        assertEquals(expectedGermanyZone, germanyZone);
    }

    private static List<BorderDirectionRecord> getBorderDirectionRecordsByCountry(List<BorderDirectionRecord> borderDirectionRecords, String country) {
        return borderDirectionRecords.stream()
            .filter(borderDirectionRecord -> country.equals(borderDirectionRecord.getBorderFrom()) || country.equals(borderDirectionRecord.getBorderTo()))
            .toList();
    }

    private static TsoInfos createTsoInfos(String name, String eic) {
        TsoInfos tso = new TsoInfos();
        tso.setName(name);
        tso.setEic(eic);
        return tso;
    }
}

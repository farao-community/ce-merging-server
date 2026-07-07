/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.inputs;

import com.farao_community.farao.ce_merging.base_case_improvement.mockdata.ReferenceProgramTestData;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"ReferenceProgramTest"})
class ReferenceProgramTest {
    private ReferenceProgram refProgram;

    @Configuration
    @Profile("ReferenceProgramTest")
    static class ContextConfiguration {

        @Bean
        public RegionConfiguration regionConfiguration() {
            RegionConfiguration regionConfiguration = new RegionConfiguration();
            Map<String, String> areasId = new HashMap<>();
            areasId.put("N1", "ONE");
            areasId.put("N12", "TWELVE");
            areasId.put("N8", "EIGHT");
            areasId.put("N0", "ZERO");
            areasId.put("N25", "TWENTYFIVE");
            regionConfiguration.setId("CE");
            regionConfiguration.setName("CE");
            regionConfiguration.setAreasIn(areasId);
            return regionConfiguration;
        }
    }

    @Autowired
    private RegionConfiguration regionConfiguration;

    @BeforeEach
    void setUp() {
        refProgram = ReferenceProgramTestData.createReferenceProgram();

    }

    @Test
    void getGlobalNetPositionOfArea() {
        assertEquals(1400., refProgram.getAreaGlobalNetPosition("ONE"), 0.);
        assertEquals(-800, refProgram.getAreaGlobalNetPosition("TWELVE"), 0.);
        assertEquals(0., refProgram.getAreaGlobalNetPosition("incorrect_id"), 0.);
    }

    @Test
    void getRelativeNetPositionOfArea() {
        assertEquals(1500, refProgram.getAreaNetPositionInRegion("ONE", regionConfiguration), 0.);
        assertEquals(-800, refProgram.getAreaNetPositionInRegion("TWELVE", regionConfiguration), 0.);
        assertEquals(0., refProgram.getAreaNetPositionInRegion("incorrect_id", regionConfiguration), 0.);
    }

    @Test
    void getOutNetPositionOfArea() {
        assertEquals(-100, refProgram.getAreaNetPositionOutRegion("ONE", regionConfiguration), 0.);
        assertEquals(0., refProgram.getAreaNetPositionOutRegion("TWELVE", regionConfiguration), 0.);
        assertEquals(0., refProgram.getAreaNetPositionOutRegion("incorrect_id", regionConfiguration), 0.);
    }

    @Test
    void getNetPositionOfRegion() {
        assertEquals(0., refProgram.computeAllNetPositionsInRegion(regionConfiguration).values().stream().mapToDouble(Double::doubleValue).sum(), 0.);
        assertEquals(-100, refProgram.computeAllGlobalNetPositions(regionConfiguration).values().stream().mapToDouble(Double::doubleValue).sum(), 0.);
        assertEquals(-100, refProgram.computeAllNetPositionsOutRegion(regionConfiguration).values().stream().mapToDouble(Double::doubleValue).sum(), 0.);
    }

    @Test
    void outRegionAreasNetPositionsTest() {
        Map<String, Double> areasOutNetPositions = refProgram.computeGlobalNetPositionsForOutAreas(regionConfiguration);
        assertEquals(2, areasOutNetPositions.size());
        assertEquals(1000, areasOutNetPositions.get("NINE"), 0.);
        assertEquals(-900, areasOutNetPositions.get("ELEVEN"), 0.);
    }
}

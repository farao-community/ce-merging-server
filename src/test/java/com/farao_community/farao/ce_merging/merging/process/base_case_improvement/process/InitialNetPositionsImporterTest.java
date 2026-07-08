/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import test_utils.CeTestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InitialNetPositionsImporterTest {
    private InputStream initialNetPositionFile;
    private RegionConfiguration regionConfiguration;

    @BeforeEach
    void setUp() throws IOException {
        initialNetPositionFile = getClass().getResourceAsStream("/bci/20190618_initialNetPositions.json");

        ObjectMapper objectMapper = new ObjectMapper();
        File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        String jsonConfig = new String(readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);
    }

    @Test
    void getInRegionNetPositionsTest() {
        FlowByAreaMap initialNetPositions = InitialNetPositionsImporter.getInRegionNetPositions(initialNetPositionFile, regionConfiguration);
        assertEquals(26, initialNetPositions.size());
        assertEquals(5258.87, initialNetPositions.get("AREA_NUMBER1_EIC"), 0.1);
    }
}

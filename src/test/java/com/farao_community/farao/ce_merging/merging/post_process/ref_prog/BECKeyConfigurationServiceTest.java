/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.BECKeyConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.RegionConfigurationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@SpringBootTest
class BECKeyConfigurationServiceTest {

    @Autowired
    private BECKeyConfigurationService becKeyConfigurationService;

    @MockitoBean
    RegionConfigurationService regionConfigurationService = mock(RegionConfigurationService.class);

    @BeforeEach
    void setUp() throws IOException {
        File resource = new ClassPathResource("gridDefaultConfigurations/region_configuration.json").getFile();
        String jsonConfig = new String(Files.readAllBytes(resource.toPath()));
        ObjectMapper objectMapper = new ObjectMapper();
        RegionConfigurationDto regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfigurationDto.class);
        when(regionConfigurationService.getConfiguration(Mockito.any())).thenReturn(new JsonRegionConfiguration(regionConfiguration));
    }

    @Test
    void parseSharingKeysBEC() throws Exception {
        List<BecByBoundaryDto> sharingKeysBEC = becKeyConfigurationService.getConfiguration(OffsetDateTime.now()).getBecByBoundaries();
        assertEquals(17, sharingKeysBEC.size());
        sharingKeysBEC.forEach(becByBoundary -> assertEquals(12, becByBoundary.getCoefficientByCountry().size()));
        assertEquals(3, sharingKeysBEC.stream().filter(becByBoundary -> becByBoundary.getBorder().getOutArea().equals("10YAT-APG------L")).count());
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.BECKeyConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.RegionConfigurationService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.mockBECKeyConfigurationRepository;
import static test_utils.CeTestUtils.mockRegionConfigurationRepository;

class BECKeyConfigurationServiceTest {

    private static final String REGION_CONFIGURATION_JSON = "gridDefaultConfigurations/region_configuration.json";
    private static final OffsetDateTime TEST_DATE = OffsetDateTime.parse("2023-11-03T00:30Z");

    RegionConfigurationService regionConfigurationService = new RegionConfigurationService(mockRegionConfigurationRepository());
    private final BECKeyConfigurationService becKeyConfigurationService = new BECKeyConfigurationService(regionConfigurationService, mockBECKeyConfigurationRepository());

    @Test
    void parseSharingKeysBEC() {
        final List<BecByBoundaryDto> sharingKeysBEC = becKeyConfigurationService.getConfiguration(TEST_DATE).getBecByBoundaries();
        assertEquals(25, sharingKeysBEC.size());
        sharingKeysBEC.forEach(becByBoundary -> assertEquals(14, becByBoundary.getCoefficientByCountry().size()));
        assertEquals(3, sharingKeysBEC.stream().filter(becByBoundary -> becByBoundary.getBorder().getOutArea().equals("10YAT-APG------L")).count());
    }
}

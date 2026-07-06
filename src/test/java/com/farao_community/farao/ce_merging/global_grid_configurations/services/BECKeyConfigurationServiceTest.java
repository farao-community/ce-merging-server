/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonBecConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class BECKeyConfigurationServiceTest {

    private final RegionConfigurationService rcService = mock(RegionConfigurationService.class);

    private GridConfigurationRepository<BECKeyConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    private final BECKeyConfigurationService service = new BECKeyConfigurationService(rcService, repository);

    @Test
    void shouldThrowWhenParsingEmptyFile() throws IOException {

        when(rcService.getConfiguration(any()))
            .thenReturn(new JsonRegionConfiguration(new RegionConfigurationDto()));

        assertThatThrownBy(() -> service.parseBecSharingKeys(BEGINNING_OF_2000, null))
            .isValidServiceException()
            .hasMessage("Could not parse sharing keys BEC file from class resources");
    }

    @Test
    void shouldHaveCommonMethodsWorking() throws IOException {
        when(rcService.getConfiguration(any()))
            .thenReturn(new JsonRegionConfiguration(new RegionConfigurationDto()));

        new ConfigurationServicesTestHelper<>(service,
                                              new BECKeyConfigurationRecord(),
                                              JsonBecConfiguration.class)
            .testAllAbstractMethods();
    }
}

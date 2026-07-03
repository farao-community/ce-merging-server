/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

class RegionConfigurationServiceTest {
    final GridConfigurationRepository<RegionConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    final RegionConfigurationService service = new RegionConfigurationService(repository);

    @Test
    void shouldHaveCommonMethodsWorking() throws IOException {
        new ConfigurationServicesTestHelper<>(service,
                                              new RegionConfigurationRecord(),
                                              JsonRegionConfiguration.class)
            .testAllAbstractMethods();
    }
}

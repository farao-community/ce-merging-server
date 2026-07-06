/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;

class VirtualHubsConfigurationServiceTest {

    final GridConfigurationRepository<VirtualHubsConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    final VirtualHubsConfigurationService service = new VirtualHubsConfigurationService(repository);

    @Test
    void shouldGetJsonConfigCallingRecord() throws IOException {

        final VirtualHubsConfigurationRecord mock = mock(VirtualHubsConfigurationRecord.class);

        when(repository.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(mock);

        service.getConfiguration(BEGINNING_OF_2000);

        verify(mock).getConfigurationJson();
    }

    @Test
    void shouldHaveCommonMethodsWorking() throws IOException {
        new ConfigurationServicesTestHelper<>(service,
                                              new VirtualHubsConfigurationRecord(),
                                              VirtualHubsConfiguration.class)
            .testAllAbstractMethods();
    }

}

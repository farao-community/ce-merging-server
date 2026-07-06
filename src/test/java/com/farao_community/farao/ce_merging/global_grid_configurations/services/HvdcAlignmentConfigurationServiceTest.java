/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.CeTestUtils.byteContentOf;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class HvdcAlignmentConfigurationServiceTest {

    private final GridConfigurationRepository<HvdcAlignmentConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    private final GridConfigurationRepository<VirtualHubsConfigurationRecord> vhRepo = mock(GridConfigurationRepository.class);
    private final VirtualHubsConfigurationService vhService = new VirtualHubsConfigurationService(vhRepo);
    private final HvdcAlignmentConfigurationService service = new HvdcAlignmentConfigurationService(vhService, repository);

    @Test
    void shouldThrowIfCoupleNotInVirtualHubs() throws IOException {

        when(repository.save(any())).thenReturn(new HvdcAlignmentConfigurationRecord());

        assertThatThrownBy(() -> service.publish(
            new MockMultipartFile("testfile", byteContentOf("gridDefaultConfigurations/hvdc-xnode-alignment-configuration_invalid.json")),
            BEGINNING_OF_2000, BEGINNING_OF_2000)
        ).isValidServiceException()
            .hasCauseExactlyInstanceOf(CeMergingException.class);

    }

    @Test
    void shouldHaveCommonMethodsWorking() throws IOException {
        when(vhRepo.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(null);

        new ConfigurationServicesTestHelper<>(service, new HvdcAlignmentConfigurationRecord(), JsonHvdcAlignmentConfiguration.class)
            .testAllAbstractMethods();
    }
}

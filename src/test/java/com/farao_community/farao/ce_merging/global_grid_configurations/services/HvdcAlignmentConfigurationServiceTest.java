/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class HvdcAlignmentConfigurationServiceTest {

    private final GridConfigurationRepository<HvdcAlignmentConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    private final VirtualHubsConfigurationService vhService = mock(VirtualHubsConfigurationService.class);
    private final HvdcAlignmentConfigurationService service = new HvdcAlignmentConfigurationService(vhService);

    @Test
    void shouldThrowIfCoupleNotInVirtualHubs() throws IOException {

        service.setRepository(repository);

        when(repository.save(any())).thenReturn(new HvdcAlignmentConfigurationRecord());
        when(vhService.getConfiguration(any())).thenReturn(new VirtualHubsConfiguration());

        assertThatThrownBy(() -> service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                                                 BEGINNING_OF_2000, BEGINNING_OF_2000))
            .isValidServiceException()
            .hasCauseExactlyInstanceOf(CeMergingException.class);

    }
}

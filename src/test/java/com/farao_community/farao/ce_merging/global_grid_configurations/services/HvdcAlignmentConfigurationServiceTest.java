/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.HvdcAlignmentConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.VirtualHubsConfigurationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.CeTestUtils.byteContentOf;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class HvdcAlignmentConfigurationServiceTest {

    private final HvdcAlignmentConfigurationRepository repository = mock(HvdcAlignmentConfigurationRepository.class);
    private final VirtualHubsConfigurationRepository vhRepo = mock(VirtualHubsConfigurationRepository.class);
    private final VirtualHubsConfigurationService vhService = new VirtualHubsConfigurationService(vhRepo);
    private final HvdcAlignmentConfigurationService service = new HvdcAlignmentConfigurationService(vhService, repository);

    @Test
    void shouldThrowIfCoupleNotInVirtualHubs() {

        when(repository.save(any())).thenReturn(new HvdcAlignmentConfigurationRecord());

        assertThatThrownBy(() -> service.publish(
                new MockMultipartFile("testfile", byteContentOf("gridDefaultConfigurations/hvdc-xnode-alignment-configuration_invalid.json")),
                BEGINNING_OF_2000, BEGINNING_OF_2000)
        ).isValidServiceException()
                .hasCauseExactlyInstanceOf(CeMergingException.class);

    }

    @Test
    void getConfigurationRecordFromFileTest() throws IOException {
        final byte[] content = "{}".getBytes();
        final MockMultipartFile file = new MockMultipartFile("test", content);
        final OffsetDateTime validFrom = OffsetDateTime.parse("2026-10-01T12:00:00+02:00");
        final OffsetDateTime validTo = OffsetDateTime.parse("2026-10-02T12:00:00+02:00");
        final LocalDateTime validFromLocal = LocalDateTime.parse("2026-10-01T10:00:00");
        final LocalDateTime validToLocal = LocalDateTime.parse("2026-10-02T10:00:00");
        final HvdcAlignmentConfigurationRecord record = service.getConfigurationRecordFromFile(file, validFrom, validTo);
        Assertions.assertThat(record.getValidFrom()).isEqualTo(validFromLocal);
        Assertions.assertThat(record.getValidTo()).isEqualTo(validToLocal);
    }
}

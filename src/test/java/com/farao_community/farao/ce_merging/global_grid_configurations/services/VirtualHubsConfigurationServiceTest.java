/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.VirtualHubsConfigurationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;

class VirtualHubsConfigurationServiceTest {

    final VirtualHubsConfigurationRepository repository = mock(VirtualHubsConfigurationRepository.class);
    final VirtualHubsConfigurationService service = new VirtualHubsConfigurationService(repository);

    @Test
    void shouldGetJsonConfigCallingRecord() {

        final VirtualHubsConfigurationRecord mock = mock(VirtualHubsConfigurationRecord.class);

        when(repository.findLatestValidOfType(
                any(LocalDateTime.class))
        ).thenReturn(mock);

        service.getConfiguration(BEGINNING_OF_2000);

        verify(mock).getConfigurationJson();
    }

    @Test
    void getConfigurationRecordFromFileTest() throws IOException {
        final byte[] content = "<Configuration/>".getBytes();
        final MockMultipartFile file = new MockMultipartFile("test", content);
        final OffsetDateTime validFrom = OffsetDateTime.parse("2026-10-01T12:00:00+02:00");
        final OffsetDateTime validTo = OffsetDateTime.parse("2026-10-02T12:00:00+02:00");
        final LocalDateTime validFromLocal = LocalDateTime.parse("2026-10-01T10:00:00");
        final LocalDateTime validToLocal = LocalDateTime.parse("2026-10-02T10:00:00");
        final VirtualHubsConfigurationRecord record = service.getConfigurationRecordFromFile(file, validFrom, validTo);
        Assertions.assertThat(record.getValidFrom()).isEqualTo(validFromLocal);
        Assertions.assertThat(record.getValidTo()).isEqualTo(validToLocal);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.BECKeyConfigurationRepository;
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
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class BECKeyConfigurationServiceTest {

    private final RegionConfigurationService rcService = mock(RegionConfigurationService.class);

    private final BECKeyConfigurationRepository repository = mock(BECKeyConfigurationRepository.class);
    private final BECKeyConfigurationService service = new BECKeyConfigurationService(rcService, repository);

    @Test
    void shouldThrowWhenParsingEmptyFile() {

        when(rcService.getConfiguration(any()))
                .thenReturn(new JsonRegionConfiguration(new RegionConfigurationDto()));

        assertThatThrownBy(() -> service.parseBecSharingKeys(BEGINNING_OF_2000, null))
                .isValidServiceException()
                .hasMessage("Could not parse sharing keys BEC file from class resources");
    }

    @Test
    void getConfigurationRecordFromFileTest() throws IOException {
        when(rcService.getConfiguration(any()))
                .thenReturn(new JsonRegionConfiguration(new RegionConfigurationDto()));
        final byte[] content = "Empty test file".getBytes();
        final MockMultipartFile file = new MockMultipartFile("test", content);
        final OffsetDateTime validFrom = OffsetDateTime.parse("2026-10-01T12:00:00+02:00");
        final OffsetDateTime validTo = OffsetDateTime.parse("2026-10-02T12:00:00+02:00");
        final LocalDateTime validFromLocal = LocalDateTime.parse("2026-10-01T10:00:00");
        final LocalDateTime validToLocal = LocalDateTime.parse("2026-10-02T10:00:00");
        final BECKeyConfigurationRecord becConfig = service.getConfigurationRecordFromFile(file, validFrom, validTo);
        Assertions.assertThat(becConfig.getValidFrom()).isEqualTo(validFromLocal);
        Assertions.assertThat(becConfig.getValidTo()).isEqualTo(validToLocal);
    }
}

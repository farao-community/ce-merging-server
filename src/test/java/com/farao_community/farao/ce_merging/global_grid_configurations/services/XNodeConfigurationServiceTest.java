/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.XNodeConfigurationRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class XNodeConfigurationServiceTest {

    private final XNodeConfigurationRepository repository = Mockito.mock(XNodeConfigurationRepository.class);
    private final XNodeConfigurationService service = new XNodeConfigurationService(repository);

    @Test
    void getConfigurationRecordFromFile() throws IOException {
        final byte[] content = "<xnodes/>".getBytes();
        final MockMultipartFile file = new MockMultipartFile("test", content);
        final OffsetDateTime validFrom = OffsetDateTime.parse("2026-10-01T12:00:00+02:00");
        final OffsetDateTime validTo = OffsetDateTime.parse("2026-10-02T12:00:00+02:00");
        final LocalDateTime validFromLocal = LocalDateTime.parse("2026-10-01T10:00:00");
        final LocalDateTime validToLocal = LocalDateTime.parse("2026-10-02T10:00:00");
        final XNodeConfigurationRecord xnodeConfig = service.getConfigurationRecordFromFile(file, validFrom, validTo);
        Assertions.assertThat(xnodeConfig.getValidFrom()).isEqualTo(validFromLocal);
        Assertions.assertThat(xnodeConfig.getValidTo()).isEqualTo(validToLocal);
    }
}

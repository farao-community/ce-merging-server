/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;

@Service
public class RegionConfigurationService extends AbstractGridConfigurationService<RegionConfigurationRecord, JsonRegionConfiguration> {

    private final GridConfigurationRepository<RegionConfigurationRecord> repository;

    public RegionConfigurationService(final GridConfigurationRepository<RegionConfigurationRecord> repository) {
        this.repository = repository;
    }

    @Override
    protected GridConfigurationRepository<RegionConfigurationRecord> getRepository() {
        return repository;
    }

    @Override
    protected JsonRegionConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) throws IOException {
        final RegionConfigurationDto regionConfiguration = JsonUtils.read(RegionConfigurationDto.class,
                                                                          getDefaultConfigFileStream());
        return new JsonRegionConfiguration(regionConfiguration);
    }

    @Override
    protected JsonRegionConfiguration getJsonConfigurationFromRecord(final RegionConfigurationRecord cfgRecord) {
        return new JsonRegionConfiguration(cfgRecord.getRegionConfiguration());
    }

    @Override
    protected RegionConfigurationRecord getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                                       final OffsetDateTime validFrom,
                                                                       final OffsetDateTime validTo) throws IOException {

        final byte[] cfgFileContent = configurationFile.getBytes();
        final RegionConfigurationDto regionConfiguration = JsonUtils.read(RegionConfigurationDto.class,
                                                                          new ByteArrayInputStream(cfgFileContent));

        return new RegionConfigurationRecord(generateUuidString(),
                                             validFrom.toLocalDateTime(),
                                             validTo.toLocalDateTime(),
                                             LocalDateTime.now(UTC_ZONE_ID),
                                             regionConfiguration);
    }
}

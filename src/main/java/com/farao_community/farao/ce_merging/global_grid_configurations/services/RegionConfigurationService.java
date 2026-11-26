/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.RegionConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toUtcLocalDateTime;

@Service
public class RegionConfigurationService extends AbstractGridConfigurationService<RegionConfigurationRecord, JsonRegionConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConfigurationService.class);

    private final RegionConfigurationRepository repository;

    public RegionConfigurationService(final RegionConfigurationRepository repository) {
        this.repository = repository;
    }

    public JsonRegionConfiguration getConfiguration(OffsetDateTime targetDate) {
        try {
            RegionConfigurationRecord configRecord = repository.findLatestValidOfType(toUtcLocalDateTime(targetDate));
            LOGGER.info("configuration retrieved from server");
            return getJsonConfigurationFromRecord(configRecord);
        } catch (final Exception e) {
            LOGGER.warn("configuration cannot be retrieved, default configuration will be used, cause : ", e);
            return getDefaultJsonConfiguration(targetDate);
        }
    }

    public void publish(final MultipartFile configurationFile,
                        final OffsetDateTime validFrom,
                        final OffsetDateTime validTo) {
        try {
            repository.save(getConfigurationRecordFromFile(configurationFile, validFrom, validTo));
        } catch (final Exception e) {
            LOGGER.error("Configuration cannot be published to server");
            throw new CeMergingException("Configuration could not be published, file or dates could be invalid.", e);
        }
    }

    @Override
    protected JsonRegionConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) {
        try {
            final RegionConfigurationDto regionConfiguration = JsonUtils.read(RegionConfigurationDto.class,
                                                                              getDefaultConfigFileStream());
            return new JsonRegionConfiguration(regionConfiguration);
        } catch (IOException e) {
            throw new CeMergingException("Default configuration not found, cause: ", e);
        }
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
                                             toUtcLocalDateTime(validFrom),
                                             toUtcLocalDateTime(validTo),
                                             LocalDateTime.now(UTC_ZONE_ID),
                                             regionConfiguration);
    }
}

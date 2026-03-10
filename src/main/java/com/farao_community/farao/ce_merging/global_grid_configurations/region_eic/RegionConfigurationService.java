/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.region_eic;

import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.abstraction.AbstractGridConfigurationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@AllArgsConstructor
@Slf4j
public class RegionConfigurationService extends AbstractGridConfigurationService<RegionConfigurationRecord, JsonRegionConfiguration> {

    @Override
    protected JsonRegionConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.read(JsonRegionConfiguration.class, getDefaultFileStream());
    }

    @Override
    protected JsonRegionConfiguration getConfigurationfromRecord(final RegionConfigurationRecord cfgRecord) {
        return new JsonRegionConfiguration(cfgRecord.getRegionConfiguration());
    }

    @Override
    protected RegionConfigurationRecord buildFromFile(final MultipartFile configurationFile,
                                                      final OffsetDateTime validFrom,
                                                      final OffsetDateTime validTo) throws IOException {

        final String cfgFileContent = getTextContent(configurationFile);
        final RegionConfigurationDto regionConfiguration = JsonUtils.read(RegionConfigurationDto.class,
                                                                          cfgFileContent);

        return new RegionConfigurationRecord(generateId(),
                                             validFrom.toLocalDateTime(),
                                             validTo.toLocalDateTime(),
                                             LocalDateTime.now(),
                                             regionConfiguration);
    }
}

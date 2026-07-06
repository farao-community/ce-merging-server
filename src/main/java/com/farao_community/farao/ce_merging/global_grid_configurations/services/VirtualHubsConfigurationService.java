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
import com.powsybl.openrao.virtualhubs.json.JsonVirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class VirtualHubsConfigurationService extends AbstractGridConfigurationService<VirtualHubsConfigurationRecord, VirtualHubsConfiguration> {

    private final GridConfigurationRepository<VirtualHubsConfigurationRecord> repository;

    public VirtualHubsConfigurationService(final GridConfigurationRepository<VirtualHubsConfigurationRecord> repository) {
        this.repository = repository;
    }

    @Override
    protected GridConfigurationRepository<VirtualHubsConfigurationRecord> getRepository() {
        return repository;
    }

    @Override
    protected VirtualHubsConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) throws IOException {
        return XmlVirtualHubsConfiguration.importConfiguration(getDefaultConfigFileStream());
    }

    @Override
    protected VirtualHubsConfiguration getJsonConfigurationFromRecord(final VirtualHubsConfigurationRecord cfgRecord) {
        return  JsonVirtualHubsConfiguration.importConfiguration(
            new ByteArrayInputStream(cfgRecord.getConfigurationJson().getBytes(UTF_8))
        );
    }

    @Override
    protected VirtualHubsConfigurationRecord getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                                            final OffsetDateTime validFrom,
                                                                            final OffsetDateTime validTo) throws IOException {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(configurationFile
                                                                                                           .getInputStream());
        final StringWriter cfgWriter = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(cfgWriter, configuration);

        return new VirtualHubsConfigurationRecord(generateUuidString(),
                                                  validFrom.toLocalDateTime(),
                                                  validTo.toLocalDateTime(),
                                                  LocalDateTime.now(UTC_ZONE_ID),
                                                  cfgWriter.toString());
    }
}

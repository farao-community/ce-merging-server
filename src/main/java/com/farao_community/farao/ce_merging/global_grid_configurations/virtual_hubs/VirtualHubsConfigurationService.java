/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.virtual_hubs;

import com.farao_community.farao.ce_merging.global_grid_configurations.abstraction.AbstractGridConfigurationService;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.json.JsonVirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
@AllArgsConstructor
public class VirtualHubsConfigurationService extends AbstractGridConfigurationService<VirtualHubsConfigurationRecord, VirtualHubsConfiguration> {

    @Override
    protected VirtualHubsConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        return XmlVirtualHubsConfiguration.importConfiguration(getDefaultFileStream());
    }

    @Override
    protected VirtualHubsConfiguration getConfigurationfromRecord(final VirtualHubsConfigurationRecord cfgRecord) {
        return  JsonVirtualHubsConfiguration.importConfiguration(new ByteArrayInputStream(cfgRecord.getConfigurationJson()
                                                                                                     .getBytes()));
    }

    @Override
    protected VirtualHubsConfigurationRecord buildFromFile(final MultipartFile configurationFile,
                                             final OffsetDateTime validFrom,
                                             final OffsetDateTime validTo) throws IOException {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(configurationFile
                                                                                                           .getInputStream());
        final StringWriter cfgWriter = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(cfgWriter, configuration);

        return new VirtualHubsConfigurationRecord(UUID.randomUUID(),
                                                  validFrom.toLocalDateTime(),
                                                  validTo.toLocalDateTime(),
                                                  LocalDateTime.now(),
                                                  cfgWriter.toString());
    }
}

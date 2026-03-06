/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.virtual_hubs;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.json.JsonVirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
public class VirtualHubsConfigurationService {
    private static final String VIRTUAL_HUBS_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/virtual-hubs-default-config.xml";

    private final VirtualHubsConfigurationRepository repository;

    public VirtualHubsConfigurationService(VirtualHubsConfigurationRepository repository) {
        this.repository = repository;
    }

    public byte[] getVirtualHubsConfigurationBytes(final OffsetDateTime targetDate) {
        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            JsonVirtualHubsConfiguration.exportConfiguration(bos, getVirtualHubsConfiguration(targetDate));
            return bos.toByteArray();
        } catch (final Exception e) {
            final String errorMsg = "Virtual hubs configuration cannot be retrieved.";
            log.error(errorMsg);
            throw new CeMergingException(errorMsg);
        }
    }

    public VirtualHubsConfiguration getVirtualHubsConfiguration(final OffsetDateTime targetDate) throws IOException {
        try {
            final VirtualHubsConfigurationRecord cfg = repository.findLastPublishedValidBetween(targetDate.toLocalDateTime(),
                                                                                          targetDate.toLocalDateTime());

            return JsonVirtualHubsConfiguration.importConfiguration(new ByteArrayInputStream(cfg.getConfigurationJson()
                                                                                                 .getBytes()));
        } catch (final Exception e) {
            log.warn("Virtual hubs configuration cannot be retrieved, default virtual hubs configuration will be used");
            return XmlVirtualHubsConfiguration.importConfiguration(new ClassPathResource(VIRTUAL_HUBS_DEFAULT_CONFIGURATION)
                                                                       .getInputStream());
        }
    }

    public void publishVirtualHubsConfiguration(MultipartFile configurationFile, OffsetDateTime validFrom, OffsetDateTime validTo) {
        try {
            final String configurationJson = getJsonConfigurationFromMultipart(configurationFile);
            repository.save(new VirtualHubsConfigurationRecord(UUID.randomUUID(),
                                                               validFrom.toLocalDateTime(),
                                                               validTo.toLocalDateTime(),
                                                               LocalDateTime.now(),
                                                               configurationJson));
        } catch (final IOException e) {
            log.error("VirtualHubs configuration could not be published");
            throw new CeMergingException("VirtualHubs configuration could not be published, File or dates could be invalid.");
        }
    }

    private String getJsonConfigurationFromMultipart(final MultipartFile configurationFile) throws IOException {
        final VirtualHubsConfiguration configuration = XmlVirtualHubsConfiguration.importConfiguration(configurationFile
                                                                                                           .getInputStream());
        final StringWriter writer = new StringWriter();
        JsonVirtualHubsConfiguration.exportConfiguration(writer, configuration);
        return writer.toString();
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeConfigDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.XNodeConfigurationRepository;
import com.farao_community.farao.ce_merging.xsd.xnodes.Xnodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;

@Service

public class XNodeConfigurationService extends AbstractGridConfigurationService<XNodeConfigurationRecord, JsonXNodeConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(XNodeConfigurationService.class);
    private final XNodeConfigurationRepository repository;

    public XNodeConfigurationService(final XNodeConfigurationRepository repository) {
        this.repository = repository;
    }

    public JsonXNodeConfiguration getConfiguration(OffsetDateTime targetDate) {
        try {
            XNodeConfigurationRecord configRecord = repository.findLatestValidOfType(targetDate.toLocalDateTime());
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
    protected JsonXNodeConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) {
        try {
            final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, getDefaultFileBytes());
            return new JsonXNodeConfiguration(fromXnodeEntityToDtoList(xnodes));
        } catch (IOException e) {
            throw new CeMergingException("Default configuration not found, cause: ", e);
        }
    }

    @Override
    protected JsonXNodeConfiguration getJsonConfigurationFromRecord(final XNodeConfigurationRecord xNodesConfig) {
        return new JsonXNodeConfiguration(xNodesConfig.getXNodeList());
    }

    @Override
    protected XNodeConfigurationRecord getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                                      final OffsetDateTime validFrom,
                                                                      final OffsetDateTime validTo) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, configurationFile.getInputStream().readAllBytes());
        final List<XnodeConfigDto> xNodeList = fromXnodeEntityToDtoList(xnodes);
        return new XNodeConfigurationRecord(generateUuidString(),
                                            validFrom.toLocalDateTime(),
                                            validTo.toLocalDateTime(),
                                            LocalDateTime.now(UTC_ZONE_ID),
                                            xNodeList);
    }

    private List<XnodeConfigDto> fromXnodeEntityToDtoList(final Xnodes xnodes) {
        return xnodes
                .getXnode()
                .stream()
                .map(XnodeConfigDto::fromXNodeEntity)
                .toList();
    }
}

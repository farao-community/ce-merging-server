/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.XnodeMapper;
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
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toUtcLocalDateTime;

@Service

public class XNodeConfigurationService extends AbstractGridConfigurationService<XNodeConfigurationRecord, JsonXNodeConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(XNodeConfigurationService.class);
    private final XNodeConfigurationRepository repository;
    private final XnodeMapper xnodeMapper;

    public XNodeConfigurationService(final XNodeConfigurationRepository repository,
                                     final XnodeMapper xnodeMapper) {
        this.repository = repository;
        this.xnodeMapper = xnodeMapper;
    }

    public JsonXNodeConfiguration getConfiguration(OffsetDateTime targetDate) {
        try {
            XNodeConfigurationRecord configRecord = repository.findLatestValidOfType(toUtcLocalDateTime(targetDate));
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
            return new JsonXNodeConfiguration(fromXnodesToDtoList(xnodes));
        } catch (IOException e) {
            throw new CeMergingException("Default configuration not found, cause: ", e);
        }
    }

    @Override
    protected JsonXNodeConfiguration getJsonConfigurationFromRecord(final XNodeConfigurationRecord xNodesConfig) {
        return new JsonXNodeConfiguration(xnodeMapper.mapToXnodeDtoList(xNodesConfig.getXNodeList()));
    }

    @Override
    protected XNodeConfigurationRecord getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                                      final OffsetDateTime validFrom,
                                                                      final OffsetDateTime validTo) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, configurationFile.getInputStream().readAllBytes());
        final List<XnodeConfigDto> xNodeList = fromXnodesToDtoList(xnodes);
        return new XNodeConfigurationRecord(generateUuidString(),
                                            toUtcLocalDateTime(validFrom),
                                            toUtcLocalDateTime(validTo),
                                            LocalDateTime.now(UTC_ZONE_ID),
                                            xnodeMapper.mapToXnodeList(xNodeList));
    }

    private List<XnodeConfigDto> fromXnodesToDtoList(final Xnodes xnodes) {
        return xnodes
                .getXnode()
                .stream()
                .map(XnodeConfigDto::fromXNodeEntity)
                .toList();
    }
}

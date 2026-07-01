/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeConfigDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
import com.farao_community.farao.ce_merging.xsd.Xnodes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;

@Service

public class XNodeConfigurationService extends AbstractGridConfigurationService<XNodeConfigurationRecord, JsonXNodeConfiguration> {

    @Override
    protected JsonXNodeConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, getDefaultFileBytes());
        return new JsonXNodeConfiguration(fromXnodesEntityToDtoList(xnodes));
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
        final List<XnodeConfigDto> xNodeList = fromXnodesEntityToDtoList(xnodes);
        return new XNodeConfigurationRecord(generateUuidString(),
                                            validFrom.toLocalDateTime(),
                                            validTo.toLocalDateTime(),
                                            LocalDateTime.now(UTC_ZONE_ID),
                                            xNodeList);
    }

    private List<XnodeConfigDto> fromXnodesEntityToDtoList(final Xnodes xnodes) {
        return xnodes
            .getXnode()
            .stream()
            .map(XnodeConfigDto::fromXNodeEntity)
            .toList();
    }
}

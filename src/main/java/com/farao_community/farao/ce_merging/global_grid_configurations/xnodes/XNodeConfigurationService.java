/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.xnodes;

import com.farao_community.farao.ce_merging.common.util.serialization.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.abstraction.AbstractGridConfigurationService;
import com.rte_france.gsr.Xnodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class XNodeConfigurationService extends AbstractGridConfigurationService<XNodeConfigurationRecord, JsonXNodeConfiguration> {

    @Override
    protected JsonXNodeConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, getDefaultFileBytes());
        return new JsonXNodeConfiguration(fromXnodeEntityToDtoList(xnodes));
    }

    @Override
    protected JsonXNodeConfiguration getConfigurationfromRecord(final XNodeConfigurationRecord cfgRecord) {
        return new JsonXNodeConfiguration(cfgRecord.getXNodeList());
    }

    @Override
    protected XNodeConfigurationRecord buildFromFile(final MultipartFile configurationFile,
                                                     final OffsetDateTime validFrom,
                                                     final OffsetDateTime validTo) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, configurationFile.getInputStream().readAllBytes());
        final List<XnodeDto> xNodeList = fromXnodeEntityToDtoList(xnodes);
        return new XNodeConfigurationRecord(generateId(),
                                            validFrom.toLocalDateTime(),
                                            validTo.toLocalDateTime(),
                                            LocalDateTime.now(),
                                            xNodeList);
    }

    private List<XnodeDto> fromXnodeEntityToDtoList(final Xnodes xnodes) {
        return xnodes
            .getXnode()
            .stream()
            .map(xnode -> new XnodeDto(xnode.getName(),
                                       xnode.getArea1(),
                                       xnode.getSubarea1(),
                                       xnode.getArea2(),
                                       xnode.getSubarea2())
            )
            .toList();
    }
}

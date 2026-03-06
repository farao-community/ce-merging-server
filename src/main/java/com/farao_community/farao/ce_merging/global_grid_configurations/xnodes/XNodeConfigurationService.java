/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.xnodes;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.serialization.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import com.rte_france.gsr.Xnodes;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class XNodeConfigurationService {

    private static final String XNODES_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/cvg-xnodes-default-config.xml";
    private final XNodeConfigurationRepository repository;

    public void publishXNodesConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        try {
            final Xnodes xnodes = JaxbUtils.readBytes(Xnodes.class, configurationFile.getInputStream().readAllBytes());
            final List<XnodeDto> xNodeList = getXnodeDtos(xnodes);
            repository.save(new XNodeConfigurationRecord(UUID.randomUUID().toString(),
                                                         validFrom.toLocalDateTime(),
                                                         validTo.toLocalDateTime(),
                                                         LocalDateTime.now(),
                                                         xNodeList));
        } catch (final Exception e) {
            log.error("XNodes configuration could not be published");
            throw new CeMergingException("XNodes configuration could not be published, File or dates could be invalid.");
        }
    }

    private List<XnodeDto> getXnodeDtos(final Xnodes xnodes) {
        return xnodes.getXnode().stream().map(xnode ->
                new XnodeDto(xnode.getName(),
                             xnode.getArea1(),
                             xnode.getSubarea1(),
                             xnode.getArea2(),
                             xnode.getSubarea2()))
            .toList();
    }

    public byte[] retrieveXNodesConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.writeInBytes(JsonXNodeConfiguration.class, getXNodesConfiguration(targetDate));
    }

    public JsonXNodeConfiguration getXNodesConfiguration(final OffsetDateTime targetDate) throws IOException {
        try {
            final XNodeConfigurationRecord cfg = repository.findLastPublishedValid(targetDate.toLocalDateTime());
            return new JsonXNodeConfiguration(cfg.getXNodeList());
        } catch (final Exception e) {
            return getDefaultConfiguration(targetDate);
        }
    }

    private JsonXNodeConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        log.warn("No valid XNode configuration found for the date {}, default XNodes configuration will be used", targetDate);
        final Xnodes xnodes = JaxbUtils.readBytes(Xnodes.class,  new ClassPathResource(XNODES_DEFAULT_CONFIGURATION).getInputStream().readAllBytes());
        return new JsonXNodeConfiguration(getXnodeDtos(xnodes));
    }

    public static List<Xnode> importConfiguration(final InputStream is) throws IOException {
        final Xnodes xnodes = JaxbUtils.readBytes(Xnodes.class, is.readAllBytes());
        if (xnodes != null && !xnodes.getXnode().isEmpty()) {
            return xnodes
                .getXnode()
                .stream()
                .map(xnode -> new Xnode(xnode.getName(),
                                        xnode.getArea1(),
                                        xnode.getSubarea1(),
                                        xnode.getArea2(),
                                        xnode.getSubarea2()))
                .toList();
        } else {
            log.warn("XNodes config is empty");
            return new ArrayList<>();
        }
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.VirtualHubsAlignmentCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.repository.HvdcAlignmentConfigurationRepository;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;
import static java.util.function.Predicate.not;

@Service
public class HvdcAlignmentConfigurationService extends AbstractGridConfigurationService<HvdcAlignmentConfigurationRecord, JsonHvdcAlignmentConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcAlignmentConfigurationService.class);
    private final VirtualHubsConfigurationService virtualHubsConfigurationService;
    private final HvdcAlignmentConfigurationRepository repository;

    public HvdcAlignmentConfigurationService(final VirtualHubsConfigurationService virtualHubsConfigurationService,
                                             final HvdcAlignmentConfigurationRepository repository) {
        this.virtualHubsConfigurationService = virtualHubsConfigurationService;
        this.repository = repository;
    }

    public JsonHvdcAlignmentConfiguration getConfiguration(OffsetDateTime targetDate) {
        try {
            HvdcAlignmentConfigurationRecord configRecord = repository.findLatestValidOfType(targetDate.toLocalDateTime());
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
    protected JsonHvdcAlignmentConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) {
        try {
            return JsonUtils.read(JsonHvdcAlignmentConfiguration.class, getDefaultConfigFileStream());
        } catch (IOException e) {
            throw new CeMergingException("Default configuration not found, cause: ", e);
        }
    }

    @Override
    protected JsonHvdcAlignmentConfiguration getJsonConfigurationFromRecord(final HvdcAlignmentConfigurationRecord cfgRecord) {
        return new JsonHvdcAlignmentConfiguration(cfgRecord.getHvdcXNodeAlignmentCouplesDto(),
                                                  cfgRecord.getZeroFlowNodeDtos(),
                                                  cfgRecord.getDkHvdcXnodes(),
                                                  cfgRecord.getDefaultSlackNode());
    }

    @Override
    protected HvdcAlignmentConfigurationRecord getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                                              final OffsetDateTime validFrom,
                                                                              final OffsetDateTime validTo) throws IOException {

        final JsonHvdcAlignmentConfiguration alignmentConfiguration = JsonUtils.read(JsonHvdcAlignmentConfiguration.class,
                                                                                     configurationFile);

        final List<VirtualHubsAlignmentCoupleDto> xNodeCouples = alignmentConfiguration.getHvdcXNodeAlignment();
        assertXNodesArePresentInVirtualHubs(validFrom, xNodeCouples);

        return new HvdcAlignmentConfigurationRecord(
                generateUuidString(),
                validFrom.toLocalDateTime(),
                validTo.toLocalDateTime(),
                LocalDateTime.now(UTC_ZONE_ID),
                xNodeCouples,
                alignmentConfiguration.getSetZeroFlowNodes(),
                alignmentConfiguration.getDkHvdcXnodes(),
                alignmentConfiguration.getDefaultSlackNode());
    }

    private void assertXNodesArePresentInVirtualHubs(final OffsetDateTime targetDate,
                                                     final List<VirtualHubsAlignmentCoupleDto> couples) {
        final List<VirtualHub> virtualHubs = virtualHubsConfigurationService
                .getConfiguration(targetDate)
                .getVirtualHubs();

        couples.forEach(couple -> assertCoupleInHubs(couple, virtualHubs));
    }

    private static void assertCoupleInHubs(final VirtualHubsAlignmentCoupleDto couple,
                                           final List<VirtualHub> virtualHubs) {
        if (isNotInVirtualHubs(couple, virtualHubs)) {
            throw new CeMergingException("XNode couple (%s,%s) should be present in the virtual hubs configuration"
                                                 .formatted(couple.getRecessiveXNode(), couple.getReferenceXNode()));
        }
    }

    private static boolean isNotInVirtualHubs(final VirtualHubsAlignmentCoupleDto couple,
                                              final List<VirtualHub> virtualHubs) {
        return virtualHubs.isEmpty() || virtualHubs.stream()
                .map(VirtualHub::nodeName)
                .filter(Objects::nonNull)
                .filter(not(String::isBlank))
                .noneMatch(node -> node.equals(couple.getRecessiveXNode())
                                   || node.equals(couple.getReferenceXNode()));
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;
import static java.util.function.Predicate.not;

@Service
public class HvdcAlignmentConfigurationService extends AbstractGridConfigurationService<HvdcAlignmentConfigurationRecord, JsonHvdcAlignmentConfiguration> {
    private static final String XNODE_NOT_FOUND = "%s XNode %s should be present in the virtual hubs configuration";

    private final VirtualHubsConfigurationService virtualHubsConfigurationService;

    public HvdcAlignmentConfigurationService(final VirtualHubsConfigurationService virtualHubsConfigurationService) {
        this.virtualHubsConfigurationService = virtualHubsConfigurationService;
    }

    @Override
    protected JsonHvdcAlignmentConfiguration getDefaultJsonConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.read(JsonHvdcAlignmentConfiguration.class, getDefaultConfigFileStream());
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

        final List<HvdcAlignmentXNodeCoupleDto> xNodeCouples = alignmentConfiguration.getHvdcXNodeAlignment();
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
                                                     final List<HvdcAlignmentXNodeCoupleDto> couples) throws IOException {
        final List<VirtualHub> virtualHubs = virtualHubsConfigurationService
            .getConfiguration(targetDate)
            .getVirtualHubs();

        couples.forEach(couple -> assertCoupleInHubs(couple, virtualHubs));
    }

    private static void assertCoupleInHubs(final HvdcAlignmentXNodeCoupleDto couple,
                                           final List<VirtualHub> virtualHubs) {
        final String reference = couple.getReferenceXNode();
        final String recessive = couple.getRecessiveXNode();

        if (isNotVirtualHub(recessive, virtualHubs)) {
            throw new CeMergingException(XNODE_NOT_FOUND.formatted("Recessive", recessive));
        }

        if (isNotVirtualHub(reference, virtualHubs)) {
            throw new CeMergingException(XNODE_NOT_FOUND.formatted("Reference", reference));
        }
    }

    private static boolean isNotVirtualHub(final String node, final List<VirtualHub> virtualHubs) {
        return virtualHubs.stream()
            .map(VirtualHub::nodeName)
            .filter(not(String::isBlank))
            .noneMatch(node::equals);
    }
}

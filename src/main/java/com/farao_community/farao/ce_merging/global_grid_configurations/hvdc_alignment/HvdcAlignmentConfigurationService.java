/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.abstraction.AbstractGridConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.virtual_hubs.VirtualHubsConfigurationService;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Predicate;

@Service
@Slf4j
@AllArgsConstructor
public class HvdcAlignmentConfigurationService extends AbstractGridConfigurationService<HvdcAlignmentConfigurationRecord, JsonHvdcAlignmentConfiguration> {

    private final VirtualHubsConfigurationService virtualHubsConfigurationService;

    @Override
    protected JsonHvdcAlignmentConfiguration getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.read(JsonHvdcAlignmentConfiguration.class, getDefaultFileStream());
    }

    @Override
    protected JsonHvdcAlignmentConfiguration getConfigurationfromRecord(final HvdcAlignmentConfigurationRecord cfgRecord) {
        return new JsonHvdcAlignmentConfiguration(cfgRecord.getHvdcXNodeAlignmentCouplesDto(),
                                                  cfgRecord.getZeroFlowNodeDtos(),
                                                  cfgRecord.getDkHvdcXnodes(),
                                                  cfgRecord.getDefaultSlackNode());
    }

    @Override
    protected HvdcAlignmentConfigurationRecord buildFromFile(final MultipartFile configurationFile,
                                                             final OffsetDateTime validFrom,
                                                             final OffsetDateTime validTo) throws IOException {

        final JsonHvdcAlignmentConfiguration cfg = JsonUtils.read(JsonHvdcAlignmentConfiguration.class,
                                                                  configurationFile);

        final List<HvdcAlignmentXNodeCoupleDto> xNodeCouples = cfg.getHvdcXNodeAlignment();
        assertXNodesArePresentInVirtualHubs(validFrom, xNodeCouples);

        return new HvdcAlignmentConfigurationRecord(
            generateId(),
            validFrom.toLocalDateTime(),
            validTo.toLocalDateTime(),
            LocalDateTime.now(),
            xNodeCouples,
            cfg.getSetZeroFlowNodes(),
            cfg.getDkHvdcXnodes(),
            cfg.getDefaultSlackNode());
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
        final Predicate<VirtualHub> isRecessiveXNode = h -> h.nodeName().equals(couple.getRecessiveXNode());
        final Predicate<VirtualHub> isReferenceXNode = h -> h.nodeName().equals(couple.getReferenceXNode());

        if (virtualHubs.stream().noneMatch(isRecessiveXNode)) {
            throw new CeMergingException(String.format("Recessive XNode %s should be present in the virtual hubs Configurations", couple.getRecessiveXNode()));
        }

        if (virtualHubs.stream().noneMatch(isReferenceXNode)) {
            throw new CeMergingException(String.format("Reference XNode %s should be present in the virtual hubs Configurations", couple.getReferenceXNode()));
        }
    }
}

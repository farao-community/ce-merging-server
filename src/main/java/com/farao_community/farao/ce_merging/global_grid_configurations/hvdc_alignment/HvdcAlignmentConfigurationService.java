/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.virtual_hubs.VirtualHubsConfigurationService;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@Slf4j
@AllArgsConstructor
public class HvdcAlignmentConfigurationService {

    private static final String HVDC_XNODE_ALIGNMENT_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/hvdc-xnode-alignment-configuration.json";
    private final HvdcAlignmentConfigRepository repository;
    private final VirtualHubsConfigurationService virtualHubsConfigurationService;

    public void publishHvdcXNodeAlignmentConfiguration(final MultipartFile configurationFile,
                                                       final OffsetDateTime validFrom,
                                                       final OffsetDateTime validTo) {
        try {
            final JsonHvdcAlignmentConfiguration configuration = JsonUtils.read(JsonHvdcAlignmentConfiguration.class,
                                                                                configurationFile);

            final List<HvdcAlignmentXNodeCoupleDto> xNodeCouples = configuration.getHvdcXNodeAlignment();
            assertXNodesArePresentInVirtualHubs(validFrom, xNodeCouples);

            final List<ZeroFlowNodeDto> zeroFlowNodes = configuration.getSetZeroFlowNodes();
            final List<String> dkHvdcXnodes = configuration.getDkHvdcXnodes();
            final String defaultSlackNode = configuration.getDefaultSlackNode();

            repository.save(new HvdcAlignmentConfigurationRecord(
                UUID.randomUUID().toString(),
                validFrom.toLocalDateTime(),
                validTo.toLocalDateTime(),
                LocalDateTime.now(),
                xNodeCouples,
                zeroFlowNodes,
                dkHvdcXnodes,
                defaultSlackNode));
        } catch (CeMergingException e) {
            throw e;
        } catch (Exception e) {
            String errorMessage = "HVDC XNode alignment configuration could not be published, File or dates could be invalid.";
            log.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void assertXNodesArePresentInVirtualHubs(final OffsetDateTime validFrom,
                                                     final List<HvdcAlignmentXNodeCoupleDto> hvdcAlignmentXNodeCoupleList) {
        try {
            final List<VirtualHub> virtualHubs = virtualHubsConfigurationService
                .getVirtualHubsConfiguration(validFrom)
                .getVirtualHubs();

            hvdcAlignmentXNodeCoupleList.forEach(couple -> {
                final Predicate<VirtualHub> isRecessiveXNode = h -> h.nodeName().equals(couple.getRecessiveXNode());
                final Predicate<VirtualHub> isReferenceXNode = h -> h.nodeName().equals(couple.getReferenceXNode());

                if (virtualHubs.stream().noneMatch(isRecessiveXNode)) {
                    throw new CeMergingException(String.format("Recessive XNode %s should be present in the virtual hubs Configurations valid at this date: %s .", couple.getRecessiveXNode(), validFrom));
                }

                if (virtualHubs.stream().noneMatch(isReferenceXNode)) {
                    throw new CeMergingException(String.format("Reference XNode %s should be present in the virtual hubs Configurations valid at this date: %s .", couple.getReferenceXNode(), validFrom));
                }
            });
        } catch (final CeMergingException e) {
            throw e;
        } catch (final Exception e) {
            final String errorMessage = "Error occurred while retrieving virtual hubs from configuration to check XNodes for Hvdc Alignment are present in virtual hubs configurations";
            log.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    public byte[] retrieveHvdcXNodeAlignmentConfiguration(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.writeInBytes(JsonHvdcAlignmentConfiguration.class,
                                      getHvdcXNodeAlignmentConfiguration(targetDate));
    }

    public JsonHvdcAlignmentConfiguration getHvdcXNodeAlignmentConfiguration(final OffsetDateTime targetDate) throws IOException {
        try {
            final HvdcAlignmentConfigurationRecord cfg = repository.findLastPublishedValid(targetDate.toLocalDateTime());
            return new JsonHvdcAlignmentConfiguration(cfg.getHvdcXNodeAlignmentCouplesDto(),
                                                      cfg.getZeroFlowNodeDtos(),
                                                      cfg.getDkHvdcXnodes(),
                                                      cfg.getDefaultSlackNode());
        } catch (final Exception e) {
            log.warn("No HVDC XNode alignment configuration could be found on the config repository, Default configuration will be used");
            return JsonUtils.read(JsonHvdcAlignmentConfiguration.class,
                                  new ClassPathResource(HVDC_XNODE_ALIGNMENT_DEFAULT_CONFIGURATION).getInputStream());
        }
    }

}

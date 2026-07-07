/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.VirtualHubsAlignmentCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.xsd.Xnodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.util.CountryCodeUtils.mapKsToXk;
import static com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration.importConfiguration;

public final class TaskTestUtils {

    public static void setTaskDefaultConfigurations(final MergingTask task) {
        try {
            setTaskVirtualHubsConfiguration(task);
            setTaskXnodesConfig(task);
            setTaskRegionConfiguration(task);
            setHvdcXNodeAlignmentConfiguration(task);
        } catch (Exception e) {
            // Should not happen
        }
    }

    private static InputStream getGridConfigStream(final String fileName) throws IOException {
        return new ClassPathResource("gridDefaultConfigurations/%s".formatted(fileName)).getInputStream();
    }

    public static List<XnodeConfig> importXnodeConfiguration(final InputStream is) throws IOException {
        final Xnodes xnodes = JaxbUtils.readFromBytes(Xnodes.class, is.readAllBytes());
        return Optional.ofNullable(xnodes)
            .orElse(new Xnodes())
            .getXnode()
            .stream()
            .map(XnodeConfig::fromXNodeEntity)
            .toList();
    }

    private static void setTaskXnodesConfig(final MergingTask task) throws IOException {
        List<XnodeConfig> xnodes = importXnodeConfiguration(getGridConfigStream("cvg-xnodes-default-config.xml"));
        task.getConfigurations().setXnodeList(xnodes);
    }

    private static void setTaskVirtualHubsConfiguration(final MergingTask task) throws IOException {
        final VirtualHubsConfiguration cfg = importConfiguration(getGridConfigStream("virtual-hubs-default-config.xml"));
        final List<VirtualHubRecord> virtualHubRecords = mapToVirtualHubRecords(cfg.getVirtualHubs());
        adaptCountryCodeForVirtualHub(virtualHubRecords);
        task.getConfigurations().setVirtualHubList(virtualHubRecords);

        final List<BorderDirectionRecord> borderDirectionRecords = mapToBorderDirectionRecords(cfg.getBorderDirections());
        adaptCountryCodeForBorderDirection(borderDirectionRecords);
        task.getConfigurations().setBorderDirectionRecords(borderDirectionRecords);
    }

    private static void setTaskRegionConfiguration(final MergingTask task) throws IOException {
        final String jsonConfig = new String(getGridConfigStream("region_configuration.json").readAllBytes());
        final RegionConfiguration regionConfiguration = new ObjectMapper().readValue(jsonConfig, RegionConfiguration.class);
        task.getConfigurations().setRegionConfiguration(regionConfiguration);
    }

    private static void setHvdcXNodeAlignmentConfiguration(final MergingTask task) throws IOException {
        final JsonHvdcAlignmentConfiguration cfg = JsonUtils.read(JsonHvdcAlignmentConfiguration.class,
                                                                  getGridConfigStream("hvdc-xnode-alignment-configuration.json"));

        final Configurations configurations = task.getConfigurations();

        final List<VirtualHubsAlignmentCoupleDto> couplesDto = cfg.getHvdcXNodeAlignment();
        final List<VirtualHubsAlignmentCouple> couples = mapToHvdcAlignmentXNodeCouples(couplesDto);
        configurations.setVirtualHubsAlignmentCouples(couples);

        final List<ZeroFlowNodeDto> zeroFlowNodeDtos = cfg.getSetZeroFlowNodes();
        final List<ZeroFlowNode> zeroFlowNodes = mapToZeroFlowNodes(zeroFlowNodeDtos);
        configurations.setZeroFlowNodes(zeroFlowNodes);

        configurations.setDkHvdcXnodes(cfg.getDkHvdcXnodes());
        configurations.setDefaultSlackNode(cfg.getDefaultSlackNode());
    }

    private static List<VirtualHubRecord> mapToVirtualHubRecords(final List<VirtualHub> virtualHubs) {
        return virtualHubs.stream()
            .map(virtualHub -> new VirtualHubRecord(virtualHub.code(), virtualHub.eic(), virtualHub.nodeName(), virtualHub.relatedMa().code(), virtualHub.relatedMa().eic()))
            .toList();
    }

    private static List<BorderDirectionRecord> mapToBorderDirectionRecords(final List<BorderDirection> borderDirections) {
        return borderDirections.stream()
            .map(borderDirection -> new BorderDirectionRecord(borderDirection.from(), borderDirection.to()))
            .toList();
    }

    private static List<VirtualHubsAlignmentCouple> mapToHvdcAlignmentXNodeCouples(final List<VirtualHubsAlignmentCoupleDto> couplesDto) {
        return couplesDto.stream()
            .map(coupleDto -> new VirtualHubsAlignmentCouple(coupleDto.getReferenceXNode(), coupleDto.getRecessiveXNode()))
            .toList();
    }

    private static List<ZeroFlowNode> mapToZeroFlowNodes(final List<ZeroFlowNodeDto> zeroFlowNodeDtos) {
        return zeroFlowNodeDtos.stream()
            .map(zeroFlowNodeDto -> new ZeroFlowNode(zeroFlowNodeDto.getXnode(), zeroFlowNodeDto.getCountryCode()))
            .toList();
    }

    private static void adaptCountryCodeForVirtualHub(final List<VirtualHubRecord> virtualHubList) {
        virtualHubList.forEach(virtualHubRecord -> virtualHubRecord.setRelatedMaCode(mapKsToXk(virtualHubRecord.getRelatedMaCode())));
    }

    private static void adaptCountryCodeForBorderDirection(final List<BorderDirectionRecord> borderDirectionRecords) {
        borderDirectionRecords.forEach(borderDirectionRecord -> {
            borderDirectionRecord.setBorderFrom(mapKsToXk(borderDirectionRecord.getBorderFrom()));
            borderDirectionRecord.setBorderTo(mapKsToXk(borderDirectionRecord.getBorderTo()));
        });
    }
}

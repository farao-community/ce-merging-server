/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.HvdcAlignmentXNodeCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.xsd.Xnodes;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.openrao.virtualhubs.BorderDirection;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import com.powsybl.openrao.virtualhubs.xml.XmlVirtualHubsConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.util.CountryCodeUtils.mapKsToXk;

public final class TaskTestUtils {

    public static void setTaskDefaultConfigurations(MergingTask task) {
        try {
            setTaskVirtualHubsConfiguration(task);
            setTaskXnodesConfig(task);
            setTaskRegionConfiguration(task);
            setHvdcXNodeAlignmentConfiguration(task);
        } catch (Exception e) {
            // Should not happen
        }
    }

    public static void setLoadflowParameters(MergingTask task, String loadflowParametersFile) throws IOException {
        LoadFlowParameters loadFlowParameters = JsonLoadFlowParameters.read(new ClassPathResource(loadflowParametersFile).getInputStream());
        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
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

    private static void setTaskXnodesConfig(MergingTask task) throws IOException {
        String xnodesConfigDefaultLocation = "gridDefaultConfigurations/cvg-xnodes-default-config.xml";
        List<XnodeConfig> xnodes = importXnodeConfiguration(new ClassPathResource(xnodesConfigDefaultLocation).getInputStream());
        task.getConfigurations().setXnodeList(xnodes);
    }

    private static void setTaskVirtualHubsConfiguration(MergingTask task) throws IOException {
        String virtualHubsDefaultConfigLocation = "gridDefaultConfigurations/virtual-hubs-default-config.xml";
        VirtualHubsConfiguration virtualHubsDefaultConfiguration = XmlVirtualHubsConfiguration.importConfiguration(new ClassPathResource(virtualHubsDefaultConfigLocation).getInputStream());
        List<VirtualHubRecord> virtualHubRecords = mapToVirtualHubRecords(virtualHubsDefaultConfiguration.getVirtualHubs());
        adaptCountryCodeForVirtualHub(virtualHubRecords);
        task.getConfigurations().setVirtualHubList(virtualHubRecords);

        List<BorderDirectionRecord> borderDirectionRecords = mapToBorderDirectionRecords(virtualHubsDefaultConfiguration.getBorderDirections());
        adaptCountryCodeForBorderDirection(borderDirectionRecords);
        task.getConfigurations().setBorderDirectionRecords(borderDirectionRecords);
    }

    private static void setTaskRegionConfiguration(MergingTask task) throws IOException {
        String jsonConfig = new String(new ClassPathResource("gridDefaultConfigurations/region_configuration.json").getInputStream().readAllBytes());
        ObjectMapper objectMapper = new ObjectMapper();
        RegionConfiguration regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);
        task.getConfigurations().setRegionConfiguration(regionConfiguration);
    }

    private static void setHvdcXNodeAlignmentConfiguration(MergingTask taskEntity) throws IOException {
        JsonHvdcAlignmentConfiguration jsonHvdcAlignmentConfiguration = JsonUtils.read(JsonHvdcAlignmentConfiguration.class, new ClassPathResource("gridDefaultConfigurations/hvdc-xnode-alignment-configuration.json").getInputStream());

        Configurations configurations = taskEntity.getConfigurations();

        List<HvdcAlignmentXNodeCoupleDto> hvdcAlignmentXNodeCoupleDtos = jsonHvdcAlignmentConfiguration.getHvdcXNodeAlignment();
        List<HvdcAlignmentXNodeCouple> hvdcAlignmentXNodeCouples = mapToHvdcAlignmentXNodeCouples(hvdcAlignmentXNodeCoupleDtos);
        configurations.setVirtualHubsAlignmentCouples(hvdcAlignmentXNodeCouples);

        List<ZeroFlowNodeDto> zeroFlowNodeDtos = jsonHvdcAlignmentConfiguration.getSetZeroFlowNodes();
        List<ZeroFlowNode> zeroFlowNodes = mapToZeroFlowNodes(zeroFlowNodeDtos);
        configurations.setZeroFlowNodes(zeroFlowNodes);

        List<String> dkHvdcXnodes = jsonHvdcAlignmentConfiguration.getDkHvdcXnodes();
        configurations.setDkHvdcXnodes(dkHvdcXnodes);

        String defaultSlackNode = jsonHvdcAlignmentConfiguration.getDefaultSlackNode();
        configurations.setDefaultSlackNode(defaultSlackNode);
    }

    private static List<VirtualHubRecord> mapToVirtualHubRecords(List<VirtualHub> virtualHubs) {
        return virtualHubs.stream()
            .map(virtualHub -> new VirtualHubRecord(virtualHub.code(), virtualHub.eic(), virtualHub.nodeName(), virtualHub.relatedMa().code(), virtualHub.relatedMa().eic()))
            .toList();
    }

    private static List<BorderDirectionRecord> mapToBorderDirectionRecords(List<BorderDirection> borderDirections) {
        return borderDirections.stream()
            .map(borderDirection -> new BorderDirectionRecord(borderDirection.from(), borderDirection.to()))
            .toList();
    }

    private static List<HvdcAlignmentXNodeCouple> mapToHvdcAlignmentXNodeCouples(List<HvdcAlignmentXNodeCoupleDto> hvdcAlignmentXNodeCoupleDtos) {
        return hvdcAlignmentXNodeCoupleDtos.stream()
            .map(hvdcAlignmentXNodeCoupleDto -> new HvdcAlignmentXNodeCouple(hvdcAlignmentXNodeCoupleDto.getReferenceXNode(), hvdcAlignmentXNodeCoupleDto.getRecessiveXNode()))
            .toList();
    }

    private static List<ZeroFlowNode> mapToZeroFlowNodes(List<ZeroFlowNodeDto> zeroFlowNodeDtos) {
        return zeroFlowNodeDtos.stream()
            .map(zeroFlowNodeDto -> new ZeroFlowNode(zeroFlowNodeDto.getXnode(), zeroFlowNodeDto.getCountryCode()))
            .toList();
    }

    private static void adaptCountryCodeForVirtualHub(List<VirtualHubRecord> virtualHubList) {
        virtualHubList.forEach(virtualHubRecord -> virtualHubRecord.setRelatedMaCode(mapKsToXk(virtualHubRecord.getRelatedMaCode())));
    }

    private static void adaptCountryCodeForBorderDirection(List<BorderDirectionRecord> borderDirectionRecords) {
        borderDirectionRecords.forEach(borderDirectionRecord -> {
            borderDirectionRecord.setBorderFrom(mapKsToXk(borderDirectionRecord.getBorderFrom()));
            borderDirectionRecord.setBorderTo(mapKsToXk(borderDirectionRecord.getBorderTo()));
        });
    }
}

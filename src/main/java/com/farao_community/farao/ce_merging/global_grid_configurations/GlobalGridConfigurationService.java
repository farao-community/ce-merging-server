/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.BecByBoundaryMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.BorderDirectionMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.HvdcAlignmentXNodeCoupleMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.RegionConfigurationMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.VirtualHubMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.XnodeMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.ZeroFlowNodeMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.HvdcAlignmentXNodeCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Xnode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.BECKeyConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.HvdcAlignmentConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.RegionConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.VirtualHubsConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.XNodeConfigurationService;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

@Service
public class GlobalGridConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalGridConfigurationService.class);

    private final MergingTaskRepository repository;
    private final VirtualHubsConfigurationService virtualHubsConfigurationService;
    private final XNodeConfigurationService xNodeConfigurationService;
    private final BECKeyConfigurationService becKeyConfigurationService;
    private final RegionConfigurationService regionConfigurationService;
    private final HvdcAlignmentConfigurationService hvdcAlignmentConfigurationService;
    private final VirtualHubMapper virtualHubMapper;
    private final BorderDirectionMapper borderDirectionMapper;
    private final XnodeMapper xnodeMapper;
    private final BecByBoundaryMapper becByBoundaryMapper;
    private final RegionConfigurationMapper regionConfigurationMapper;
    private final HvdcAlignmentXNodeCoupleMapper hvdcAlignmentXNodeCoupleMapper;
    private final ZeroFlowNodeMapper zeroFlowNodeMapper;

    public GlobalGridConfigurationService(MergingTaskRepository repository,
                                          VirtualHubsConfigurationService virtualHubsConfigurationService,
                                          XNodeConfigurationService xNodeConfigurationService,
                                          BECKeyConfigurationService becKeyConfigurationService,
                                          HvdcAlignmentConfigurationService hvdcAlignmentConfigurationService,
                                          RegionConfigurationService regionConfigurationService,
                                          VirtualHubMapper virtualHubMapper,
                                          BorderDirectionMapper borderDirectionMapper,
                                          XnodeMapper xnodeMapper,
                                          BecByBoundaryMapper becByBoundaryMapper,
                                          RegionConfigurationMapper regionConfigurationMapper,
                                          HvdcAlignmentXNodeCoupleMapper hvdcAlignmentXNodeCoupleMapper,
                                          ZeroFlowNodeMapper zeroFlowNodeMapper) {
        this.repository = repository;
        this.virtualHubsConfigurationService = virtualHubsConfigurationService;
        this.xNodeConfigurationService = xNodeConfigurationService;
        this.becKeyConfigurationService = becKeyConfigurationService;
        this.hvdcAlignmentConfigurationService = hvdcAlignmentConfigurationService;
        this.regionConfigurationService = regionConfigurationService;
        this.virtualHubMapper = virtualHubMapper;
        this.borderDirectionMapper = borderDirectionMapper;
        this.xnodeMapper = xnodeMapper;
        this.becByBoundaryMapper = becByBoundaryMapper;
        this.regionConfigurationMapper = regionConfigurationMapper;
        this.hvdcAlignmentXNodeCoupleMapper = hvdcAlignmentXNodeCoupleMapper;
        this.zeroFlowNodeMapper = zeroFlowNodeMapper;
    }

    public void setConfigurations(final MergingTask task) {
        setRegionEicConfiguration(task);
        setVirtualHubsConfiguration(task);
        setHvdcXNodeAlignmentConfiguration(task);
        setXnodesConfiguration(task);
        setBecKeyConfiguration(task);
        setLoadflowParameters(task);
        repository.save(task);
    }

    private static void setLoadflowParameters(MergingTask taskEntity) {
        LoadFlowParameters loadFlowParameters;
        String path = taskEntity.getConfigurations().getAcLoadFlowParameters().getPath();
        if (path != null) {
            loadFlowParameters = JsonLoadFlowParameters.read(Paths.get(path));
            LOGGER.info("Reading loadflow parameters from {}", taskEntity.getConfigurations().getAcLoadFlowParameters().getOriginalName());
        } else {
            LOGGER.info("Reading default loadflow parameters");
            loadFlowParameters = LoadFlowParameters.load();
        }
        taskEntity.getConfigurations().setLoadFlowParameters(loadFlowParameters);
        LOGGER.info("Loadflow parameters are set in task configuration");
    }

    void setVirtualHubsConfiguration(MergingTask taskEntity) {
        try {
            VirtualHubsConfiguration virtualHubsConfiguration = virtualHubsConfigurationService.getConfiguration(taskEntity.getInputs().getTargetDate());

            final List<VirtualHub> virtualHubList = virtualHubsConfiguration.getVirtualHubs().stream()
                    .filter(virtualHub -> virtualHub.nodeName() != null)
                    .toList();
            taskEntity.getConfigurations().setVirtualHubList(virtualHubMapper.mapToVirtualHubRecordList(virtualHubList));
            taskEntity.getConfigurations().setBorderDirectionRecords(borderDirectionMapper.mapToBorderDirectionRecordList(virtualHubsConfiguration.getBorderDirections()));

            adaptCountryCodeForVirtualHub(taskEntity.getConfigurations().getVirtualHubList());
            adaptCountryCodeForBorderDirection(taskEntity.getConfigurations().getBorderDirectionRecords());

            LOGGER.info("Virtual hubs configuration is set on task configuration");
        } catch (Exception e) {
            String errorMessage = "Error occurred while trying to set Virtual hubs configuration, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    void setXnodesConfiguration(MergingTask taskEntity) {
        try {
            List<XnodeDto> xnodesDtoList = xNodeConfigurationService.getConfiguration(taskEntity.getInputs().getTargetDate()).getXNodeList();
            List<Xnode> xNodesFromConfigRepository = xnodeMapper.mapToXnodeList(xnodesDtoList);
            taskEntity.getConfigurations().setXnodeList(xNodesFromConfigRepository);
            LOGGER.info("XNodes configuration is set on task configuration");
        } catch (Exception e) {
            String errorMessage = "Error occurred while trying to set XNodes configuration, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    void setBecKeyConfiguration(MergingTask taskEntity) {
        try {
            List<BecByBoundaryDto> becByBoundaryDtoList = becKeyConfigurationService.getConfiguration(taskEntity.getInputs().getTargetDate()).getBecByBoundaries();
            List<BecByBoundary> becByBoundaryList = becByBoundaryMapper.mapToBecByBoundaryList(becByBoundaryDtoList);
            taskEntity.getConfigurations().setBecMatrixConfig(becByBoundaryList);
            LOGGER.info("BEC Key configuration is set on task configuration");
        } catch (Exception e) {
            String errorMessage = "Error occurred while trying to set BEC Key configuration, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    void setRegionEicConfiguration(MergingTask taskEntity) {
        try {
            JsonRegionConfiguration jsonRegionConfiguration = regionConfigurationService.getConfiguration(taskEntity.getInputs().getTargetDate());
            LOGGER.trace("EIC config : {}", jsonRegionConfiguration.getRegionConfiguration().toString());
            RegionConfiguration regionConfiguration = regionConfigurationMapper.mapToRegionConfiguration(jsonRegionConfiguration.getRegionConfiguration());
            LOGGER.trace("EIC config : {}", regionConfiguration.toString());
            taskEntity.getConfigurations().setRegionConfiguration(regionConfiguration);
            LOGGER.trace("EIC config : {}", taskEntity.getConfigurations().getRegionConfiguration().toString());
            repository.save(taskEntity);
            LOGGER.trace("EIC config : {}", taskEntity.getConfigurations().getRegionConfiguration().toString());
            LOGGER.info("Region EIC configuration is set on task configuration");
        } catch (Exception e) {
            String errorMessage = "Error occurred while trying to set Region EIC configuration, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    void setHvdcXNodeAlignmentConfiguration(MergingTask taskEntity) {
        try {
            JsonHvdcAlignmentConfiguration jsonHvdcAlignmentConfiguration = hvdcAlignmentConfigurationService.getConfiguration(taskEntity.getInputs().getTargetDate());
            Configurations configurations = taskEntity.getConfigurations();

            List<HvdcAlignmentXNodeCoupleDto> hvdcAlignmentXNodeCoupleDtos = jsonHvdcAlignmentConfiguration.getHvdcXNodeAlignment();
            List<HvdcAlignmentXNodeCouple> hvdcAlignmentXNodeCouples = hvdcAlignmentXNodeCoupleMapper.mapToHvdcAlignmentXNodeCoupleList(hvdcAlignmentXNodeCoupleDtos);
            configurations.setVirtualHubsAlignmentCouples(hvdcAlignmentXNodeCouples);

            List<ZeroFlowNodeDto> zeroFlowNodeDtos = jsonHvdcAlignmentConfiguration.getSetZeroFlowNodes();
            List<ZeroFlowNode> zeroFlowNodes = zeroFlowNodeMapper.mapToZeroFlowNodeList(zeroFlowNodeDtos);
            configurations.setZeroFlowNodes(zeroFlowNodes);

            List<String> dkHvdcXnodes = jsonHvdcAlignmentConfiguration.getDkHvdcXnodes();
            configurations.setDkHvdcXnodes(dkHvdcXnodes);

            String defaultSlackNode = jsonHvdcAlignmentConfiguration.getDefaultSlackNode();
            configurations.setDefaultSlackNode(defaultSlackNode);

            LOGGER.info("HVDC XNode alignment configuration is set on task configuration");
        } catch (Exception e) {
            String errorMessage = "Error occurred while trying to set HVDC XNode alignment configuration, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private static void adaptCountryCodeForVirtualHub(List<VirtualHubRecord> virtualHubList) {
        virtualHubList.forEach(virtualHubRecord -> virtualHubRecord.setRelatedMaCode(CountryUtils.mapKsToXk(virtualHubRecord.getRelatedMaCode())));
    }

    private static void adaptCountryCodeForBorderDirection(List<BorderDirectionRecord> borderDirectionRecords) {
        borderDirectionRecords.forEach(borderDirectionRecord -> {
            borderDirectionRecord.setBorderFrom(CountryUtils.mapKsToXk(borderDirectionRecord.getBorderFrom()));
            borderDirectionRecord.setBorderTo(CountryUtils.mapKsToXk(borderDirectionRecord.getBorderTo()));
        });
    }
}

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
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeConfigDto;
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
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.util.CountryCodeUtils.mapKsToXk;

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

    public GlobalGridConfigurationService(final MergingTaskRepository repository,
                                          final VirtualHubsConfigurationService virtualHubsConfigurationService,
                                          final XNodeConfigurationService xNodeConfigurationService,
                                          final BECKeyConfigurationService becKeyConfigurationService,
                                          final HvdcAlignmentConfigurationService hvdcAlignmentConfigurationService,
                                          final RegionConfigurationService regionConfigurationService,
                                          final VirtualHubMapper virtualHubMapper,
                                          final BorderDirectionMapper borderDirectionMapper,
                                          final XnodeMapper xnodeMapper,
                                          final BecByBoundaryMapper becByBoundaryMapper,
                                          final RegionConfigurationMapper regionConfigurationMapper,
                                          final HvdcAlignmentXNodeCoupleMapper hvdcAlignmentXNodeCoupleMapper,
                                          final ZeroFlowNodeMapper zeroFlowNodeMapper) {
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
        try {
            setRegionEicConfiguration(task);
            setVirtualHubsConfiguration(task);
            setHvdcXNodeAlignmentConfiguration(task);
            setXnodesConfiguration(task);
            setBecKeyConfiguration(task);
            setLoadFlowParameters(task);
            repository.save(task);
        } catch (final Exception e) {
            final String errorMessage = "Error occurred while trying to set task configuration";
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private static void setLoadFlowParameters(final MergingTask task) {
        final LoadFlowParameters loadFlowParameters = Optional.ofNullable(task.getConfigurations().getAcLoadFlowParameters())
            .map(SavedFile::getPath)
            .map(Paths::get)
            .map(JsonLoadFlowParameters::read)
            .orElse(LoadFlowParameters.load());
        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
        LOGGER.info("Loadflow parameters are set in task configuration");
    }

    void setVirtualHubsConfiguration(final MergingTask task) throws IOException {

        final VirtualHubsConfiguration virtualHubsConfiguration = virtualHubsConfigurationService.getConfiguration(task.getInputs().getTargetDate());

        final List<VirtualHub> virtualHubList = virtualHubsConfiguration
            .getVirtualHubs()
            .stream()
            .filter(virtualHub -> virtualHub.nodeName() != null)
            .toList();

        final Configurations configurations = task.getConfigurations();

        configurations.setVirtualHubList(virtualHubMapper.mapToVirtualHubRecordList(virtualHubList));
        configurations.setBorderDirectionRecords(borderDirectionMapper.mapToBorderDirectionRecordList(virtualHubsConfiguration.getBorderDirections()));

        fixKosovoVirtualHubs(configurations.getVirtualHubList());
        fixKosovoBorderDirections(configurations.getBorderDirectionRecords());
        LOGGER.info("Virtual hubs configuration is set on task configuration");

    }

    void setXnodesConfiguration(final MergingTask task) throws IOException {
        final List<XnodeConfigDto> xnodesDtoList = xNodeConfigurationService.getConfiguration(task.getInputs().getTargetDate()).getXNodesList();
        task.getConfigurations().setXnodeList(xnodeMapper.mapToXnodeList(xnodesDtoList));
        LOGGER.info("XNodes configuration is set on task configuration");
    }

    void setBecKeyConfiguration(final MergingTask task) throws IOException {
        final List<BecByBoundaryDto> becByBoundaryDtos = becKeyConfigurationService.getConfiguration(task.getInputs().getTargetDate()).getBecByBoundaries();
        task.getConfigurations().setBecMatrixConfig(becByBoundaryMapper.mapToBecByBoundaryList(becByBoundaryDtos));
        LOGGER.info("BEC Key configuration is set on task configuration");
    }

    void setRegionEicConfiguration(final MergingTask task) throws IOException {
        final JsonRegionConfiguration jsonConfig = regionConfigurationService.getConfiguration(task.getInputs().getTargetDate());
        task.getConfigurations().setRegionConfiguration(
            regionConfigurationMapper.mapToRegionConfiguration(jsonConfig.getRegionConfiguration())
        );
        LOGGER.info("Region EIC configuration is set on task configuration");
    }

    void setHvdcXNodeAlignmentConfiguration(final MergingTask task) throws IOException {
        final JsonHvdcAlignmentConfiguration jsonConfig = hvdcAlignmentConfigurationService.getConfiguration(task.getInputs()
                                                                                                                 .getTargetDate());
        final Configurations configurations = task.getConfigurations();

        configurations.setZeroFlowNodes(zeroFlowNodeMapper.mapToZeroFlowNodeList(jsonConfig.getSetZeroFlowNodes()));
        configurations.setDkHvdcXnodes(jsonConfig.getDkHvdcXnodes());
        configurations.setDefaultSlackNode(jsonConfig.getDefaultSlackNode());
        configurations.setVirtualHubsAlignmentCouples(
            hvdcAlignmentXNodeCoupleMapper.mapToHvdcAlignmentXNodeCoupleList(jsonConfig.getHvdcXNodeAlignment())
        );
    }

    private static void fixKosovoVirtualHubs(final List<VirtualHubRecord> virtualHubs) {
        virtualHubs.forEach(hub -> hub.setRelatedMaCode(mapKsToXk(hub.getRelatedMaCode())));
    }

    private static void fixKosovoBorderDirections(final List<BorderDirectionRecord> borderDirections) {
        borderDirections.forEach(direction -> {
            direction.setBorderFrom(mapKsToXk(direction.getBorderFrom()));
            direction.setBorderTo(mapKsToXk(direction.getBorderTo()));
        });
    }
}

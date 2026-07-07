/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_AND_DANISH_TSO;

@Service
public class XnodesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XnodesService.class);
    private static final String FILE_IS_SAVED_IN_TASK_ARTIFACTS = "File '{}' is saved in task '{}' artifacts";
    private static final String FILE_NAME_INFORMATION = "xnodesInformation.json";

    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;
    private final InitialImportService initialImportService;
    private final XnodesCalculation xnodesCalculation;

    public XnodesService(MergingTaskRepository tasksRepository, CeMergingConfiguration configuration, InitialImportService initialImportService, XnodesCalculation xnodesCalculation) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
        this.initialImportService = initialImportService;
        this.xnodesCalculation = xnodesCalculation;
    }

    public void checkIgmsStatus(MergingTask task) {
        Map<String, Network> networkByTsoMap = initialImportService.importInitialIgms(task);
        Map<String, XnodeInformation> xnodeInformationMap = new TreeMap<>();
        List<VirtualHubRecord> virtualHubList = task.getConfigurations().getVirtualHubList();
        List<XnodeConfig> xnodesConfigList = task.getConfigurations().getXnodeList();
        networkByTsoMap.forEach((tso, network) -> {
            xnodesCalculation.checkXnodesConfigConsistency(network, virtualHubList, xnodesConfigList);
            xnodesCalculation.fillXnodesInformation(network, tso, xnodeInformationMap, virtualHubList, xnodesConfigList, isGermanOrDanishTso(tso));
        });
        saveXnodesInformationFileInArtifacts(xnodeInformationMap, task);
        tasksRepository.save(task);
    }

    private static boolean isGermanOrDanishTso(String zoneId) {
        return GERMAN_AND_DANISH_TSO.contains(zoneId);
    }

    private void saveXnodesInformationFileInArtifacts(Map<String, XnodeInformation> xnodeInformationMap, MergingTask task) {
        XnodesCheck xnodesCheck = new XnodesCheck(xnodeInformationMap);
        Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(task), FILE_NAME_INFORMATION);
        try {
            JsonUtils.writeInPath(XnodesCheck.class, xnodesCheck, filePath);
        } catch (IOException e) {
            LOGGER.error("Unable to write xnodes information file for task {}", task.getId(), e);
            throw new CeMergingException("Failed to write Xnodes information file", e);
        }
        String fileUrl = "/tasks/%d/artifacts/xnodes-information".formatted(task.getId());
        SavedFile xnodesSavedFile = new SavedFile(FILE_NAME_INFORMATION, filePath.toString(), fileUrl);
        task.getArtifacts().putFile(ArtifactType.XNODES_INFORMATION_FILE, xnodesSavedFile);
        LOGGER.info(FILE_IS_SAVED_IN_TASK_ARTIFACTS, FILE_NAME_INFORMATION, task.getId());
    }

}

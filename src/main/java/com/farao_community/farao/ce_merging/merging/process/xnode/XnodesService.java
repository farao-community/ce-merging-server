/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)   This Source Code Form is subject to the terms of the Mozilla Public   License, v. 2.0. If a copy of the MPL was not distributed with this   file, You can obtain one at http://mozilla.org/MPL/2.0/.   SPDX-License-Identifier: MPL-2.0
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;

@Service
public class XnodesService {

    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;
    private final InitialImportService initialImportService;
    private final XnodesCalculation xnodesCalculation;

    public XnodesService(MergingTaskRepository tasksRepository,
                         CeMergingConfiguration configuration,
                         InitialImportService initialImportService,
                         XnodesCalculation xnodesCalculation) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
        this.initialImportService = initialImportService;
        this.xnodesCalculation = xnodesCalculation;
    }

    public void checkIgmsStatus(MergingTask task) {
        final Configurations configurations = task.getConfigurations();
        final Map<String, XnodeInformation> xnodeInformationMap = new TreeMap<>();
        final List<VirtualHubRecord> virtualHubList = configurations.getVirtualHubList();
        final List<XnodeConfig> xnodesConfigList = configurations.getXnodeList();
        initialImportService.importInitialIgms(task).forEach((tso, network) -> {
            xnodesCalculation.checkXnodesConfigConsistency(network, virtualHubList, xnodesConfigList);
            xnodesCalculation.fillXnodesInformation(network, tso, xnodeInformationMap, virtualHubList, xnodesConfigList);
        });
        saveArtifactFile(XNODES_INFORMATION_FILE, xnodeInformationMap, task, configuration);
        tasksRepository.save(task);
    }

}

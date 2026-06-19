/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
public class GridConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GridConfigurationService.class);

    private static void setLoadFlowParameters(final MergingTask task) {
        final LoadFlowParameters loadFlowParameters;
        final String path = task.getConfigurations().getAcLoadFlowParameters().getPath();
        if (path != null) {
            loadFlowParameters = JsonLoadFlowParameters.read(Paths.get(path));
            LOGGER.info("Reading load flow parameters from {}", task.getConfigurations().getAcLoadFlowParameters().getOriginalName());
        } else {
            LOGGER.info("Reading default load flow parameters");
            loadFlowParameters = LoadFlowParameters.load();
        }
        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
        LOGGER.info("Load flow parameters are set in task configuration");
    }

    //TODO implement the remaining functions
}

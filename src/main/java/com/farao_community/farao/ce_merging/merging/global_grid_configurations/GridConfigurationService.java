/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
@Slf4j
public class GridConfigurationService {

    private static void setLoadFlowParameters(final MergingTask task) {
        final LoadFlowParameters loadFlowParameters;
        final String path = task.getConfigurations().getAcLoadFlowParameters().getPath();
        if (path != null) {
            loadFlowParameters = JsonLoadFlowParameters.read(Paths.get(path));
            log.info("Reading load flow parameters from {}", task.getConfigurations().getAcLoadFlowParameters().getOriginalName());
        } else {
            log.info("Reading default load flow parameters");
            loadFlowParameters = LoadFlowParameters.load();
        }
        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
        log.info("Load flow parameters are set in task configuration");
    }

    //TODO implement the remaining functions
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.logs.LogsCustomisationService;
import com.farao_community.farao.ce_merging.merging.config.MergingStep;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Optional;

@Order(0)
@Service
public class GridConfigurationService extends AbstractMergingService {

    protected GridConfigurationService(final MergingTaskRepository tasksRepository,
                                       final CeMergingConfiguration configuration,
                                       final LogsCustomisationService logsCustomisationService) {
        super(tasksRepository, configuration, logsCustomisationService);
    }

    @Override
    protected MergingStep getStep() {
        return MergingStep.CONFIGURATIONS;
    }

    @Override
    protected void handleStep(final MergingTask task) {
        // most configuration is missing, to be done when grid configurations are merged
        setLoadFlowParameters(task);
    }

    private static void setLoadFlowParameters(final MergingTask task) {
        final String acConfigPath = task.getConfigurations().getAcLoadFlowParameters().getPath();
        final LoadFlowParameters loadFlowParameters = Optional.ofNullable(acConfigPath)
            .map(Paths::get)
            .map(JsonLoadFlowParameters::read)
            .orElse(LoadFlowParameters.load());

        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
    }

}

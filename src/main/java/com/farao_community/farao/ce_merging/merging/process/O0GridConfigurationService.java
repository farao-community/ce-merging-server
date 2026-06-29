/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Optional;

@Order(0)
@Service
public class O0GridConfigurationService extends AbstractMergingService {

    protected O0GridConfigurationService(final MergingTaskRepository tasksRepository,
                                         final CeMergingConfiguration configuration) {
        super(tasksRepository, configuration);
    }

    @Override
    public boolean handle(final MergingTask task) {
        // most configuration is missing, to be done when grid configurations are merged

        try {
            setLoadFlowParameters(task);
            tasksRepository.save(task);
        } catch (final Exception e) {
            throw new CeMergingException("error while setting configurations of task %d".formatted(task.getId()), e);
        }
        return false;
    }

    private static void setLoadFlowParameters(final MergingTask task) {
        final LoadFlowParameters loadFlowParameters = Optional.ofNullable(task.getConfigurations().getAcLoadFlowParameters())
            .map(SavedFile::getPath)
            .map(Paths::get)
            .map(JsonLoadFlowParameters::read)
            .orElse(LoadFlowParameters.load());

        task.getConfigurations().setLoadFlowParameters(loadFlowParameters);
    }

}

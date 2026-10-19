/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.LogsCustomisationUtils;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.MergingStep;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.OutputType;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RefProgCalculationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefProgCalculationService.class);

    private final CeMergingConfiguration configuration;
    private final MergingTaskRepository mergingTaskRepository;

    public RefProgCalculationService(final MergingTaskRepository mergingTaskRepository,
                                     final CeMergingConfiguration configuration
                                     ) {
        this.configuration = configuration;
        this.mergingTaskRepository = mergingTaskRepository;
    }

    public void computeRefProg(final MergingTask task) {
        try {
            LogsCustomisationUtils.setExtraFieldsInLogsMdc(task, MergingStep.REF_PROG);
            final PublicationDocument finalRefProgResult = RefProgCalculator.forTask(task).computeRefProg();
            saveRefProgFileInOutputs(finalRefProgResult, task);
            mergingTaskRepository.save(task);
        } catch (final Exception e) {
            final String errorMessage = String.format("RefProg computation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void saveRefProgFileInOutputs(final PublicationDocument finalRefProgResult, final MergingTask task) {
        final OutputType outputType = OutputType.REF_PROG;
        final String fileName = outputType.getFileName(task.getTargetDate());
        final String location = outputType.getLocation(task.getId());
        final SavedFile refProgSavedFile = FileStorageUtils.save(
                configuration.getOutputsDirectoryPath(task),
                fileName,
                location,
                path -> JaxbUtils.writeToPath(PublicationDocument.class, finalRefProgResult, path)
        );

        task.getOutputs().setRefProg(refProgSavedFile);
    }
}

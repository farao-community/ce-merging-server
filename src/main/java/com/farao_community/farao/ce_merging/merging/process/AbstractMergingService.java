/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILENAME_DATETIME_FMT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NUMBER_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.STRING_FORMAT;
import static java.util.Locale.FRANCE;

public abstract class AbstractMergingService implements Handler<MergingTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMergingService.class);
    protected final MergingTaskRepository tasksRepository;
    protected final CeMergingConfiguration configuration;

    protected AbstractMergingService(final MergingTaskRepository tasksRepository,
                                     final CeMergingConfiguration configuration) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
    }

    protected <T> void saveArtifactFile(final ArtifactType fileType,
                                        final T businessObject,
                                        final MergingTask task) {
        try {
            // write actual file
            final String fileName = getFileName(fileType, task);
            final Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(task), fileName);
            JsonUtils.writeInPath((Class<T>) businessObject.getClass(), businessObject, filePath);
            // put it in task data
            final SavedFile artifactFile = new SavedFile(fileName, filePath.toString(), getFileLocation(fileType, task));
            task.getArtifacts().putFile(fileType, artifactFile);
            LOGGER.info("File '{}' is saved in task {} artifacts", fileName, task.getId());
        } catch (final Exception e) {
            final String errorMessage = "error while saving %s artifact".formatted(fileType.name());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private String getFileName(final ArtifactType fileType, final MergingTask task) {
        final ZonedDateTime targetZdtParis = task.getInputs().getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
        final String dateAndTime = FILENAME_DATETIME_FMT.withLocale(FRANCE).format(targetZdtParis);
        final String fileName = fileType.getFileName();

        if (fileName.contains(STRING_FORMAT) && fileName.contains(NUMBER_FORMAT)) {
            return fileName.formatted(dateAndTime, targetZdtParis.getDayOfWeek().getValue());
        } else if (fileName.contains(STRING_FORMAT)) {
            return fileName.formatted(dateAndTime);
        } else {
            return fileName;
        }
    }

    private String getFileLocation(final ArtifactType fileType, final MergingTask task) {
        return String.format("/tasks/%d/artifacts/%s", task.getId(), fileType.getLocation());
    }

}

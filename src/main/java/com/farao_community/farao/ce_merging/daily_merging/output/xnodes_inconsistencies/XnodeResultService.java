/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.xnodes_inconsistencies;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.copyFileTo;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.OUTPUT_DATE_FORMATTER;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.OUTPUT_TIME_FORMATTER;

@Service
public class XnodeResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XnodeResultService.class);

    private static final String XNODE_INCONSISTENCIES = "xnodes-inconsistencies_%s%s_%s.json";
    private static final String ZIP_NAME = "xnodes-inconsistencies.zip";
    private static final String TEMP_DIRECTORY_PREFIX = "xnodes-inconsistencies-result";

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;

    public XnodeResultService(final CeMergingConfiguration configuration,
                              final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void createXnodesInconsistenciesZip(final DailyMergingTask dailyTask,
                                               final List<MergingTask> mergingTasks) {
        try {
            final Path resultTempPath = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
            final boolean winterDstDetected = isWinterDst(mergingTasks);
            mergingTasks.stream()
                    .filter(task -> task.getStatus().equals(TaskStatus.SUCCESS))
                    .forEach(entity -> {
                        final String fileName = buildFileName(entity, winterDstDetected);
                        final SavedFile xnodesInconsistenciesFile = entity.getArtifacts().getFile(ArtifactType.XNODES_INCONSISTENCIES);
                        if (xnodesInconsistenciesFile != null && xnodesInconsistenciesFile.getPath() != null) {
                            copyFileTo(fileName, xnodesInconsistenciesFile, resultTempPath);
                        }
                    });

            final SavedFile xnodesZip = FileStorageUtils.save(
                    configuration.getDailyOutputsDirectoryPath(dailyTask),
                    ZIP_NAME,
                    String.format("/daily-merging/tasks/%d/outputs/xnodes-zip", dailyTask.getId()),
                    path -> {
                        try {
                            Files.write(path, ZipUtils.zipDirectory(resultTempPath.toString()));
                        } catch (final IOException e) {
                            throw new CeMergingException("Cannot write xnodes inconsistencies ZIP", e);
                        }
                    }
            );
            dailyTask.getDailyOutputs().setXnodesZip(xnodesZip);

            repository.save(dailyTask);
            LOGGER.info("File '{}' is saved in task '{}' outputs", xnodesZip, dailyTask.getId());
        } catch (final IOException e) {
            LOGGER.warn("Error while creating xnodes inconsistencies ZIP of daily merging task '{}'", dailyTask.getId(), e);
        }
    }

    private String buildFileName(final MergingTask task,
                                 final boolean winterDstDetected) {
        final ZonedDateTime targetDate = task.getTargetDateInParis();
        final String dstIndexOrEmpty = winterDstDetected && task.isAtSecondDstHour() ? DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION : EMPTY;
        final String date = OUTPUT_DATE_FORMATTER.format(targetDate);
        final String time = OUTPUT_TIME_FORMATTER.format(targetDate);
        return XNODE_INCONSISTENCIES.formatted(date, dstIndexOrEmpty, time);
    }

    private boolean isWinterDst(final List<MergingTask> mergingTasks) {
        return mergingTasks.stream()
                       .filter(MergingTask::isAtDstHour)
                       .count() == 2;
    }

}

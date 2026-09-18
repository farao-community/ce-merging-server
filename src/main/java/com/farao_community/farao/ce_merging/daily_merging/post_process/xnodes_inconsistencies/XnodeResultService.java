/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.post_process.xnodes_inconsistencies;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.JSON_EXTENSION;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.formatTargetDate;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.getTargetDateAtParisZone;

@Service
public class XnodeResultService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XnodeResultService.class);
    private static final String DAYLIGHT_DUPLICATED_HOUR = "02";
    private static final String DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION = "B";
    private static final String XNODE_INCONSISTENCIES = "xnodes-inconsistencies_";
    private static final String ZIP_NAME = "xnodes-inconsistencies.zip";
    private static final String TEMP_DIRECTORY_PREFIX = "xnodes-inconsistencies-result";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;

    public XnodeResultService(final CeMergingConfiguration configuration, final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void createXnodesInconsistenciesZip(final DailyMergingTask dailyTask, final List<MergingTask> mergingTasks) {
        try {
            final Path resultTempPath = Files.createTempDirectory(TEMP_DIRECTORY_PREFIX);
            final boolean clockChange = isWinterDst(mergingTasks);
            mergingTasks.stream()
                    .filter(task -> task.getStatus().equals(TaskStatus.SUCCESS))
                    .forEach(entity -> {
                        final String newFileName = buildFileName(entity, clockChange);
                        final SavedFile xnodesInconsistenciesFile = entity.getArtifacts().getFile(ArtifactType.XNODES_INCONSISTENCIES);
                        if (xnodesInconsistenciesFile != null && xnodesInconsistenciesFile.getPath() != null) {
                            copyFileTo(newFileName, xnodesInconsistenciesFile, resultTempPath);
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

    private String buildFileName(final MergingTask task, final boolean clockChange) {
        final ZonedDateTime targetDate = getTargetDateAtParisZone(task);
        if (clockChange && isTheSecondHour(task)) {
            return XNODE_INCONSISTENCIES
                    + DATE_FORMATTER.format(targetDate)
                    + DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION
                    + TIME_FORMATTER.format(targetDate)
                    + JSON_EXTENSION;
        }
        return XNODE_INCONSISTENCIES + formatTargetDate(task) + JSON_EXTENSION;
    }

    private boolean isWinterDst(final List<MergingTask> coreMergingTaskEntityList) {
        return coreMergingTaskEntityList.stream()
                .filter(this::isWinterDstHour)
                .count() == 2;
    }

    private boolean isWinterDstHour(final MergingTask task) {
        final ZonedDateTime targetDateInEuropeZone = task.getInputs().getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
        return targetDateInEuropeZone.getHour() == Integer.parseInt(DAYLIGHT_DUPLICATED_HOUR);
    }

    private void copyFileTo(final String fileName, final SavedFile savedFile, final Path otherDirectoryPath) {
        final Path filePath = otherDirectoryPath.resolve(fileName);
        try {
            Files.createDirectories(otherDirectoryPath);
            Files.copy(Paths.get(savedFile.getPath()), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (final IOException e) {
            LOGGER.warn("Error while writing {} file in path {}", fileName, filePath, e);
        }
    }

    public boolean isTheSecondHour(final MergingTask task) {
        final ZonedDateTime targetDateInEuropeZone = getTargetDateAtParisZone(task);
        return targetDateInEuropeZone.getHour() == Integer.parseInt(DAYLIGHT_DUPLICATED_HOUR)
                && task.getInputs().getRealOffset().toString().equals("+01:00");
    }
}

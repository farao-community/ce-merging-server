/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.output.cgm;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DATE_TIME_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ZIP_EXTENSION;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.copyFileTo;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.DAYLIGHT_DUPLICATED_HOUR;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.isSuccessful;
import static java.util.Locale.FRANCE;

@Service
public class CgmResultsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmResultsService.class);
    private static final int FLOW = 100;

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository dailyMergingRepository;
    private final CgmRecognitionService cgmRecognitionService;

    public CgmResultsService(final CeMergingConfiguration configuration,
                             final DailyMergingRepository dailyMergingRepository,
                             final CgmRecognitionService cgmRecognitionService) {
        this.configuration = configuration;
        this.dailyMergingRepository = dailyMergingRepository;
        this.cgmRecognitionService = cgmRecognitionService;
    }

    public void createCgmZip(DailyMergingTask dailyTask,
                             List<MergingTask> coreMergingTaskEntityList,
                             RequestInformation requestInformation) {
        try {
            final Path cgmResultTempPath = Files.createTempDirectory("cgm-result"); // NOSONAR directories are used safely here
            final String cgmZipName = OutputUtils.generateOutputFileNameWithHour(requestInformation.getMergingDay(), dailyTask.getVersion(), FLOW, ZIP_EXTENSION);
            final String cgmResultFilePath = String.format("%s/%s", configuration.getDailyOutputsDirectoryPath(dailyTask), cgmZipName);
            final Function<SavedFile, String> fileName = cgm -> cgm.getOriginalName().replace("UC0", String.format("UC%d", dailyTask.getVersion()));

            renameCgmInDaylightCase(coreMergingTaskEntityList);
            coreMergingTaskEntityList.stream().filter(isSuccessful())
                    .map(MergingTask::getOutputs)
                    .map(Outputs::getCgm)
                    .forEach(cgm -> copyFileTo(fileName.apply(cgm), cgm, cgmResultTempPath));
            saveCgmRecognitionToCgmZip(dailyTask, cgmResultTempPath.toString(), requestInformation, coreMergingTaskEntityList);
            zipFilesAndMoveInDirectory(cgmResultTempPath.toString(), cgmResultFilePath);

            final SavedFile cgmZip = new SavedFile(cgmZipName, cgmResultFilePath, String.format("/daily-merging/tasks/%d/outputs/cgm-zip", dailyTask.getId()));
            dailyTask.getDailyOutputs().setCgmZip(cgmZip);
            dailyMergingRepository.save(dailyTask);
            LOGGER.info("File '{}' is saved in task '{}' outputs", cgmZipName, dailyTask.getId());
        } catch (final IOException e) {
            LOGGER.error("Error while creating CGM ZIP of daily merging task '{}'", dailyTask.getId());
            throw new ServiceIOException(String.format("Error while creating CGM ZIP of daily merging task '%d'.", dailyTask.getId()), e);
        }
    }

    private void saveCgmRecognitionToCgmZip(DailyMergingTask dailyTask,
                                            String cgmResultTempPath,
                                            RequestInformation requestInformation,
                                            List<MergingTask> coreMergingTaskEntityList) {
        byte[] cgmRecognitionFile = cgmRecognitionService.computeCgmRecognition(requestInformation, coreMergingTaskEntityList, dailyTask.getVersion());
        String requestTimeInterval = requestInformation.requestTimeInterval();
        OffsetDateTime mergingDay = OffsetDateTime.parse(requestTimeInterval.substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
        String cgmRecognitionOutputFileName = OutputUtils.generateOutputFileName(mergingDay, dailyTask.getVersion(), FLOW, XML_EXTENSION);
        writeBytes(cgmRecognitionFile, String.format("%s/%s", cgmResultTempPath, cgmRecognitionOutputFileName));
        LOGGER.info("File '{}' is saved in task '{}' outputs", cgmRecognitionOutputFileName, dailyTask.getId());
    }

    private void zipFilesAndMoveInDirectory(String tempDirectory,
                                            String directory) {
        writeBytes(ZipUtils.zipDirectory(tempDirectory), directory);
    }

    private static void writeBytes(byte[] bytes, final String destinationPath) {
        try (final OutputStream os = new FileOutputStream(destinationPath)) {
            os.write(bytes);
        } catch (final IOException e) {
            throw new ServiceIOException("Error while writing file in directory", e);
        }
    }

    private void renameCgmInDaylightCase(final List<MergingTask> coreMergingTaskEntityList) {
        final List<MergingTask> tasksAtTwoOClock = coreMergingTaskEntityList.stream()
                .filter(isAtDstHour())
                .toList();

        if (tasksAtTwoOClock.size() == 2) {
            final SavedFile secondCgm = tasksAtTwoOClock.stream()
                    .filter(isAtSecondDstHour())
                    .findFirst()
                    .map(MergingTask::getOutputs)
                    .map(Outputs::getCgm)
                    .orElseThrow(() -> new CeMergingException("Unexpected duplicated tasks at 2 o'clock without an offset shift"));

            final String fileNameUpdated = secondCgm.getOriginalName().substring(0, 9)
                                     + DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION
                                     + secondCgm.getOriginalName().substring(10);

            secondCgm.setOriginalName(fileNameUpdated);

        }
    }

    private Predicate<MergingTask> isAtDstHour() {
        return task -> DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).withLocale(FRANCE)
                .format(task.getTargetDateInParis())
                .startsWith(DAYLIGHT_DUPLICATED_HOUR, 9);
    }

    private Predicate<MergingTask> isAtSecondDstHour() {
        return task -> task.getTargetDate()
                               .minusHours(Integer.parseInt(task.getTargetDate().getOffset().toString().substring(0, 3)))
                               .getHour() == 1;
    }
}

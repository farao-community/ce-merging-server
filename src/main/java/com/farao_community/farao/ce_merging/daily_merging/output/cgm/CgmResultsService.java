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
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ZIP_EXTENSION;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.copyFileTo;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION;
import static com.farao_community.farao.ce_merging.common.util.OutputUtils.generateOutputFileName;
import static com.farao_community.farao.ce_merging.common.util.ZipUtils.zipDirectory;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;

@Service
public class CgmResultsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmResultsService.class);
    private static final int CGM_FLOW = 100;
    private static final int FILENAME_END_OF_DATETIME_INDEX = 9;

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;
    private final CgmRecognitionService cgmRecognitionService;

    public CgmResultsService(final CeMergingConfiguration configuration,
                             final DailyMergingRepository repository,
                             final CgmRecognitionService cgmRecognitionService) {
        this.configuration = configuration;
        this.repository = repository;
        this.cgmRecognitionService = cgmRecognitionService;
    }

    public void createCgmZip(final DailyMergingTask dailyTask,
                             final List<MergingTask> hourlyTasks,
                             final RequestInformation requestInformation) {
        try {
            final Path cgmResultTempPath = Files.createTempDirectory("cgm-result"); // NOSONAR directories are used safely here
            final String cgmZipName = generateOutputFileName(requestInformation.getMergingDay(),
                                                                     dailyTask.getVersion(),
                                                                     CGM_FLOW,
                                                                     ZIP_EXTENSION);
            final String taskOutPath = configuration.getDailyOutputsDirectoryPath(dailyTask);
            final String cgmResultFilePath = getZipDestination(taskOutPath, cgmZipName);
            final Function<SavedFile, String> fileName = cgm -> cgm.getOriginalName().replace("UC0", String.format("UC%d", dailyTask.getVersion()));

            renameCgmInDaylightCase(hourlyTasks);
            hourlyTasks.stream().filter(task -> task.getStatus() == SUCCESS)
                    .map(MergingTask::getOutputs)
                    .map(Outputs::getCgm)
                    .forEach(cgm -> copyFileTo(fileName.apply(cgm), cgm, cgmResultTempPath));
            addCgmRecognitionToZip(dailyTask, cgmResultTempPath.toString(), requestInformation, hourlyTasks);
            writeBytes(zipDirectory(cgmResultTempPath.toString()), cgmResultFilePath);

            final SavedFile cgmZip = new SavedFile(cgmZipName, cgmResultFilePath, String.format("/daily-merging/tasks/%d/outputs/cgm-zip", dailyTask.getId()));
            dailyTask.getDailyOutputs().setCgmZip(cgmZip);
            repository.save(dailyTask);
            LOGGER.info("File '{}' is saved in task '{}' outputs", cgmZipName, dailyTask.getId());
        } catch (final Exception e) {
            LOGGER.error("Error while creating CGM ZIP of daily merging task '{}'", dailyTask.getId(), e);
            throw new CeMergingException(String.format("Error while creating CGM ZIP of daily merging task '%d'.", dailyTask.getId()), e);
        }
    }

    private void addCgmRecognitionToZip(final DailyMergingTask dailyTask,
                                        final String cgmResultTempPath,
                                        final RequestInformation requestInformation,
                                        final List<MergingTask> hourlyTasks) throws JAXBException, ParserConfigurationException {
        final byte[] cgmRecognitionFile = cgmRecognitionService.computeCgmRecognition(requestInformation, hourlyTasks, dailyTask.getVersion());
        final String cgmRecognitionOutputFileName = generateOutputFileName(requestInformation.getMergingDay(),
                                                                           dailyTask.getVersion(),
                                                                           CGM_FLOW,
                                                                           XML_EXTENSION);
        writeBytes(cgmRecognitionFile, getZipDestination(cgmResultTempPath, cgmRecognitionOutputFileName));
        LOGGER.info("File '{}' is saved in task '{}' outputs", cgmRecognitionOutputFileName, dailyTask.getId());
    }

    private static void writeBytes(final byte[] bytes,
                                   final String destinationPath) {
        try (final OutputStream os = new FileOutputStream(destinationPath)) {
            os.write(bytes);
        } catch (final IOException e) {
            throw new ServiceIOException("Error while writing file in directory", e);
        }
    }

    private String getZipDestination(final String targetPath,
                                     final String zipFileName) {
        return String.format("%s/%s", targetPath, zipFileName);
    }

    private void renameCgmInDaylightCase(final List<MergingTask> hourlyTasks) {
        final List<MergingTask> tasksAtTwoOClock = hourlyTasks.stream()
                .filter(MergingTask::isAtDstHour)
                .toList();

        if (tasksAtTwoOClock.size() == 2) {
            final SavedFile secondCgm = tasksAtTwoOClock.stream()
                    .filter(MergingTask::isAtSecondDstHour)
                    .findFirst()
                    .map(MergingTask::getOutputs)
                    .map(Outputs::getCgm)
                    .orElseThrow(() -> new CeMergingException("Unexpected duplicated tasks at 2 o'clock without an offset shift"));

            final String fileNameUpdated = secondCgm.getOriginalName().substring(0, FILENAME_END_OF_DATETIME_INDEX)
                                           + DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION
                                           + secondCgm.getOriginalName().substring(FILENAME_END_OF_DATETIME_INDEX + 1);

            secondCgm.setOriginalName(fileNameUpdated);

        }
    }

}

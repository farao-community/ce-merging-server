/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement;

import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciInputs;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciTask;
import com.farao_community.farao.ce_merging.base_case_improvement.process.BciProcess;
import com.farao_community.farao.ce_merging.base_case_improvement.repository.BciTaskRepository;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.farao_community.farao.ce_merging.common.task.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.SUCCESS;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.ClassLoader.getSystemResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readString;
import static java.util.UUID.randomUUID;

@Service
public class BciService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BciService.class);
    private final CeMergingConfiguration ceMergingConfiguration;

    private final BciTaskRepository bciTaskRepository;

    private static final String EIC_CODES_DEFAULT_CONFIGURATION = "defaultConfiguration/region_configuration.json";

    public BciService(CeMergingConfiguration ceMergingConfiguration, BciTaskRepository bciTaskRepository) {
        this.ceMergingConfiguration = ceMergingConfiguration;
        this.bciTaskRepository = bciTaskRepository;
    }

    public BciTask createBciTask(final String name,
                                 final OffsetDateTime targetDate,
                                 final MultipartFile netPositionsForecast,
                                 final MultipartFile externalConstraints,
                                 final Optional<MultipartFile> feasibilityRange,
                                 final Optional<MultipartFile> initialNetPositions,
                                 final Optional<MultipartFile> alegroNetPositions,
                                 final Optional<MultipartFile> eicConfig) {

        final BciTask bciTask = bciTaskRepository.save(new BciTask());

        try {

            bciTask.setName(Optional.ofNullable(name).orElse(randomUUID().toString()));

            final String content;

            if (eicConfig.isPresent()) {
                content = new String(eicConfig.get().getBytes(), UTF_8);
            } else {
                content = readString(Paths.get(getSystemResource(EIC_CODES_DEFAULT_CONFIGURATION).toURI()));
            }

            bciTask.setRegionConfiguration(content);

            createDirectories(Paths.get(ceMergingConfiguration.getInputsDirectoryPath(bciTask)));
            createDirectories(Paths.get(ceMergingConfiguration.getOutputsDirectoryPath(bciTask))); // Prepare output directory

            final File npForecastFile = transferToInputs(netPositionsForecast, bciTask);
            final File ecFile = transferToInputs(externalConstraints, bciTask);

            final BciInputs inputs = new BciInputs(npForecastFile.getPath(), ecFile.getPath());

            transferAndSetPathIfPresent(feasibilityRange, bciTask, inputs::setFeasibilityRangePath);
            transferAndSetPathIfPresent(initialNetPositions, bciTask, inputs::setInitialNetPositionsPath);
            transferAndSetPathIfPresent(alegroNetPositions, bciTask, inputs::setAlegroNetPositionsPath);

            bciTask.setProcessTargetDate(targetDate);
            bciTask.setRealOffset(targetDate.getOffset());
            bciTask.setBciInputs(inputs);

            return bciTaskRepository.save(bciTask);
        } catch (final Exception e) {
            bciTaskRepository.delete(bciTask);
            LOGGER.error("Error during BCI task creation due to invalid data", e);
            throw new TaskNotValidException("Error during BCI task creation due to invalid data", e);
        }
    }

    private void transferAndSetPathIfPresent(final Optional<MultipartFile> multipartFile,
                                             final BciTask bciTask,
                                             final Consumer<String> pathSetter) throws IOException {
        if (multipartFile.isPresent()) {
            final File file = transferToInputs(multipartFile.get(), bciTask);
            pathSetter.accept(file.getPath());
        }
    }

    public BciTask runBciTask(long taskId) {
        final BciTask bciTask = getBciTask(taskId);
        bciTask.setStatus(RUNNING);
        bciTaskRepository.save(bciTask);
        final BciProcess bciProcess = new BciProcess(bciTask,
                                                     ceMergingConfiguration,
                                                     regionConfigFromContent(bciTask.getRegionConfiguration()));
        try {
            bciProcess.run();
            bciTask.setStatus(SUCCESS);
            bciTaskRepository.save(bciTask);
            LOGGER.info("Base Case Improvement is finished with success");
            return bciTask;
        } catch (final Exception e) {
            bciTask.setStatus(ERROR);
            bciTaskRepository.save(bciTask);
            String errorMessage = "Error in Base Case Improvement, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    public BciTask getBciTask(final Long taskId) {
        BciTask task = bciTaskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(String.format("Task %d not found", taskId)));
        handleDst(task);
        return task;
    }

    private void handleDst(final BciTask task) {
        // necessary treatment for the case of daylight:
        // the changed hour (second 2H:30) will have an offset= +2 but really should be + 1
        if (!task.getProcessTargetDate().getOffset().equals(task.getRealOffset())) {
            final LocalDateTime localTargetDate = task.getProcessTargetDate().toLocalDateTime();
            final ZoneOffset realZoneOffset = task.getRealOffset();
            task.setProcessTargetDate(OffsetDateTime.of(localTargetDate, realZoneOffset));
        }
    }

    public List<BciTask> getAllBciTasks() {
        return bciTaskRepository.findAll();
    }

    public byte[] getBciOutputs(final Long taskId) {
        final BciTask bciTask = getBciTask(taskId);
        bciTask.assertFinished();
        try {
            return Files.readAllBytes(Paths.get(ceMergingConfiguration.getOutputsDirectoryPath(bciTask),
                                                "bciOutput.json"));
        } catch (final IOException e) {
            LOGGER.error("IO exception while reading BCI outputs of task '{}'", taskId, e);
            throw new ServiceIOException(String.format("IO exception while reading BCI outputs of task '%d'", taskId), e);
        }
    }

    public void removeBciTask(long taskId) {
        BciTask task = getBciTask(taskId);
        // Delete created files in its internal database
        try {
            FileSystemUtils.deleteRecursively(Paths.get(ceMergingConfiguration.getOutputsDirectoryPath(task)));
            bciTaskRepository.deleteById(taskId);
        } catch (IOException e) {
            LOGGER.error("IO exception while deleting BCI task '{}'", taskId, e);
            throw new ServiceIOException(String.format("IO exception while deleting BCI task '%d'", taskId), e);
        }
    }

    public void removeAllBciTasks() {
        bciTaskRepository.findAll().stream()
            .map(BciTask::getId)
            .forEach(this::removeBciTask);
    }

    public byte[] getInputZip(final Long taskId) {
        return ZipUtils.zipDirectory(ceMergingConfiguration.getInputsDirectoryPath(getBciTask(taskId)));
    }

    private File transferToInputs(final MultipartFile multipartFile, final BciTask task) throws IOException {
        File file = new File(
            Paths.get(ceMergingConfiguration.getInputsDirectoryPath(task), multipartFile.getName()).toString()
        );
        multipartFile.transferTo(file); // NOSONAR directories are used safely here
        return file;
    }

    private RegionConfiguration regionConfigFromContent(final String content) {
        try {
            return new ObjectMapper()
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(content, RegionConfiguration.class);
        } catch (final JsonProcessingException e) {
            LOGGER.error("Error in Base Case Improvement {}", e.getMessage());
            throw new CeMergingException("Error in Base Case Improvement ", e);
        }
    }

}

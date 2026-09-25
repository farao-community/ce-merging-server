/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotRunException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyOutputs;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static org.apache.commons.io.FileUtils.deleteQuietly;

@Service
public class DailyMergingTasksManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyMergingTasksManagementService.class);

    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;
    private final MergingTaskManagementService service;
    private final DailyMergingService dailyMergingService;

    public DailyMergingTasksManagementService(final CeMergingConfiguration configuration,
                                              final DailyMergingRepository repository,
                                              final MergingTaskManagementService service,
                                              final DailyMergingService dailyMergingService) {
        this.configuration = configuration;
        this.dailyMergingService = dailyMergingService;
        this.repository = repository;
        this.service = service;
    }
    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        TASKS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public DailyMergingTask createDailyTask(final List<Long> ids,
                                            final MultipartFile mergingRequest,
                                            final int mergingVersion) {
        final DailyMergingTask task = new DailyMergingTask();
        task.setVersion(mergingVersion);
        final List<Long> completedTaskIds = getCompletedTaskIds(ids);
        task.setMergingTaskIds(completedTaskIds);
        repository.save(task);

        final Path dailyDirectory = Path.of(configuration.getDailyDirectoryPath(task));
        final Path inputsPath = Path.of(configuration.getDailyInputsDirectoryPath(task));
        final Path outputsPath = Path.of(configuration.getDailyOutputsDirectoryPath(task));

        try {
            Files.createDirectories(inputsPath);
            Files.createDirectories(outputsPath);
            copyMergingRequest(mergingRequest, inputsPath);
            fillTaskInputs(mergingRequest.getOriginalFilename(), task, task.getDailyInputs());
            repository.save(task);
        } catch (final Exception e) {
            LOGGER.error("Error during Daily merging task computation", e);
            deleteQuietly(dailyDirectory.toFile());
            repository.delete(task);
            throw new CeMergingException("Error during Daily merging task creation", e);
        }
        return task;
    }

    public DailyMergingTask runDailyMergingTask(final long dailyTaskId) {
        final DailyMergingTask task = getTaskById(dailyTaskId);
        try {
            if (task.getTaskStatus() == RUNNING) {
                throw new TaskAlreadyRunningException(String.format("Task %d already running, could not be run again",
                                                                    task.getId()));
            }
            task.setTaskStatus(RUNNING);
            LOGGER.info("Running daily merging task: '{}' ", task.getId());
            repository.save(task);
            final List<MergingTask> hourlyTasks = task.getMergingTaskIds()
                    .stream()
                    .map(service::getTaskById)
                    .toList();

            dailyMergingService.run(task, hourlyTasks);
            task.setTaskStatus(SUCCESS);
            LOGGER.info("Daily task: '{}' is finished with success", task.getId());
            repository.save(task);
            return task;
        } catch (final Exception e) {
            task.setTaskStatus(ERROR);
            repository.save(task);
            throw e;
        }
    }

    public List<DailyMergingTask> getAllTasks() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .toList();
    }

    public DailyMergingTask getTask(final long taskId) {
        return getTaskById(taskId);
    }

    public void deleteAllTasks() {
        repository.findAllIds().forEach(this::deleteTask);
    }

    public void deleteTask(final long taskId) {
        try {
            final DailyMergingTask task = getTaskById(taskId);
            FileSystemUtils.deleteRecursively(Paths.get(configuration.getDailyDirectoryPath(task)));
            repository.deleteById(taskId);
        } catch (final IOException e) {
            throw new CeMergingException("Error during daily merging task delete", e);
        }
    }

   /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        INPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getMergingRequest(final long taskId) {
        return getTaskById(taskId).getDailyInputs().getMergingRequest();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getDailyRefProgOutput(final long taskId) {
        return getOutputs(taskId).getRefProg();
    }

    public SavedFile getDailyMergingResponse(final long taskId) {
        return getOutputs(taskId).getMergingResponse();
    }

    public SavedFile getDailyMergingLogs(final long taskId) {
        return getOutputs(taskId).getMergingLogs();
    }

    public SavedFile getDailyCgmZip(final long taskId) {
        return getOutputs(taskId).getCgmZip();
    }

    public SavedFile getDailyGlskReport(final long taskId) {
        return getOutputs(taskId).getGlskQualityReport();
    }

    public byte[] getDailyResultPackage(final long taskId) throws IOException {
        final DailyMergingTask task = getTaskById(taskId);
        checkTaskRunned(task);
        final String fileName = "response_daily_task_" + taskId + ".json";
        final String outputDirectory = configuration.getDailyOutputsDirectoryPath(task);
        JsonUtils.writeInPath(DailyMergingTask.class, task, Paths.get(outputDirectory + "/" + fileName));
        return ZipUtils.zipDirectory(outputDirectory);

    }

    public SavedFile getMergingReport(final long taskId) {
        return getOutputs(taskId).getMergingReport();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
            INTERNAL (PRIVATE & PACKAGE-PRIVATE)
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private DailyOutputs getOutputs(final long taskId) {
        return getTaskById(taskId).getDailyOutputs();
    }

    DailyMergingTask getTaskById(final long taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(String.format("Task %d not available", taskId)));
    }

    void checkTaskRunned(final DailyMergingTask task) {
        if (task.getTaskStatus() == CREATED) {
            throw new TaskNotRunException(String.format("Task %d has not been run", task.getId()));
        } else if (task.getTaskStatus() == RUNNING) {
            throw new TaskAlreadyRunningException(String.format("Task %d currently running", task.getId()));
        }
    }

    private List<Long> getCompletedTaskIds(final List<Long> hourlyTasksIds) {
        final List<Long> completedTaskIds = new ArrayList<>();
        for (final Long taskId : hourlyTasksIds) {
            if (taskId == null) {
                throw new CeMergingException("Invalid merging task ID");
            }
            if (service.checkTaskExist(taskId)) {
                final MergingTask task = service.getTaskById(taskId);
                if (task.getStatus() == CREATED) {
                    LOGGER.warn("Merging task {} has not been run", taskId); //NOSONAR  : taskId is a Long
                } else if (task.getStatus() == TaskStatus.RUNNING) {
                    LOGGER.warn("Merging task {} is currently running", taskId); //NOSONAR  : taskId is a Long
                }
                completedTaskIds.add(taskId);
            } else {
                LOGGER.warn("Merging task {} does not exist, it will be removed from daily merge", taskId); //NOSONAR  : taskId is a Long
            }
        }
        return completedTaskIds;
    }

    private void fillTaskInputs(final String originalFilename,
                                final DailyMergingTask task,
                                final DailyInputs dailyInputs) {
        final Path inputFilePath = Path.of(configuration.getDailyInputsDirectoryPath(task), originalFilename);
        dailyInputs.setMergingRequestFilePath(inputFilePath.toString());
        dailyInputs.getMergingRequest().setLocation("/tasks/" + task.getId() + "/inputs/merging-request");
        task.setDailyInputs(dailyInputs);
    }

    private void copyMergingRequest(final MultipartFile mergingRequest,
                                    final Path taskInputPath) throws IOException {
        final String originalFilename = mergingRequest.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new CeMergingException("Merging request filename is missing");
        }
        final Path normalizedTaskInputPath = taskInputPath.toAbsolutePath().normalize();
        final String filename = Path.of(originalFilename).getFileName().toString();
        if (filename.isBlank()) {
            throw new CeMergingException("Invalid merging request filename");
        }
        final Path inputPath = normalizedTaskInputPath.resolve(filename).normalize();
        if (!inputPath.startsWith(normalizedTaskInputPath)) {
            throw new CeMergingException("Invalid merging request filename");
        }
        mergingRequest.transferTo(inputPath);
    }
}

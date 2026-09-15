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
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
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

import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.RUNNING;
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

    public DailyMergingTask createDailyTask(final List<Long> ids, final MultipartFile mergingRequest, final int mergingVersion) {
        final DailyMergingTask dailyMergingTask = new DailyMergingTask();
        dailyMergingTask.setVersion(mergingVersion);
        final List<Long> completedTaskIds = getCompletedTaskIds(ids);
        dailyMergingTask.setMergingTaskIds(completedTaskIds);
        repository.save(dailyMergingTask);
        final Path dailyDirectory = Path.of(configuration.getDailyDirectoryPath(dailyMergingTask));
        final Path inputsPath = Path.of(configuration.getDailyInputsDirectoryPath(dailyMergingTask));
        final Path outputsPath = Path.of(configuration.getDailyOutputsDirectoryPath(dailyMergingTask));
        try {
            Files.createDirectories(inputsPath);
            Files.createDirectories(outputsPath);
            copyMergingRequest(mergingRequest, inputsPath);
            fillTaskInputs(mergingRequest.getOriginalFilename(), dailyMergingTask, dailyMergingTask.getDailyInputs());
            repository.save(dailyMergingTask);
        } catch (Exception e) {
            LOGGER.error("Error during Daily merging task computation", e);
            deleteQuietly(dailyDirectory.toFile());
            repository.delete(dailyMergingTask);
            throw new CeMergingException("Error during Daily merging task creation", e);
        }
        return dailyMergingTask;
    }

    public DailyMergingTask runDailyMergingTask(long dailyTaskId) {
        DailyMergingTask dailyTask = getTaskById(dailyTaskId);
        try {
            if (dailyTask.getTaskStatus() == RUNNING) {
                throw new TaskAlreadyRunningException(String.format("Task %d already running, could not be run again",
                        dailyTask.getId()));
            }
            dailyTask.setTaskStatus(RUNNING);
            LOGGER.info("Running daily merging task: '{}' ", dailyTask.getId());
            repository.save(dailyTask);
            List<MergingTask> mergingTasks = new ArrayList<>();
            dailyTask.getMergingTaskIds().forEach(taskId -> mergingTasks.add(service.getTaskById(taskId)));
            dailyMergingService.run(dailyTask, mergingTasks);
            dailyTask.setTaskStatus(TaskStatus.SUCCESS);
            LOGGER.info("Daily task: '{}' is finished with success", dailyTask.getId());
            repository.save(dailyTask);
            return dailyTask;
        } catch (Exception e) {
            dailyTask.setTaskStatus(TaskStatus.ERROR);
            repository.save(dailyTask);
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
        repository.findAll().forEach(task -> deleteTask(task.getId()));
    }

    public void deleteTask(final long taskId) {
        try {
            final DailyMergingTask task = getTaskById(taskId);
            FileSystemUtils.deleteRecursively(Paths.get(configuration.getDailyDirectoryPath(task)));
            repository.deleteById(taskId);
        } catch (IOException e) {
            throw new CeMergingException("Error during daily merging task delete", e);
        }
    }

   /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        INPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getMergingRequest(final long taskId) {
        DailyMergingTask task = getTaskById(taskId);
        return task.getDailyInputs().getMergingRequest();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getDailyRefProgOutput(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyMergingResponse(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyMergingLogs(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyCgmZip(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getDailyGlskReport(final long taskId) {
        //TODO
        return null;
    }

    public byte[] getDailyResultPackage(final long taskId) {
        //TODO
        return null;
    }

    public SavedFile getMergingReport(final long taskId) {
        //TODO
        return null;
    }

    DailyMergingTask getTaskById(final long taskId) {
        return repository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(String.format("Task %d not available", taskId)));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   INTERNAL (PRIVATE)
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private List<Long> getCompletedTaskIds(final List<Long> ids) {
        final List<Long> completedTaskIds = new ArrayList<>();
        for (long taskId : ids) {
            if (service.checkTaskExist(taskId)) {
                MergingTask taskEntity = service.getTaskById(taskId);
                if (taskEntity.getStatus() == TaskStatus.CREATED) {
                    LOGGER.warn("Merging task {} has not been run", taskId);
                } else if (taskEntity.getStatus() == TaskStatus.RUNNING) {
                    LOGGER.warn("Merging task {} is currently running", taskId);
                }
                completedTaskIds.add(taskId);
            } else {
                LOGGER.warn("Merging task {} does not exist, it will be removed from daily merge", taskId);
            }
        }
        return completedTaskIds;
    }

    private void fillTaskInputs(final String originalFilename, final DailyMergingTask task, final DailyInputs dailyInputs) {
        final Path inputFilePath = Path.of(configuration.getDailyInputsDirectoryPath(task), originalFilename);
        dailyInputs.setMergingRequestFilePath(inputFilePath.toString());
        dailyInputs.getMergingRequest().setLocation("/tasks/" + task.getId() + "/inputs/merging-request");
        task.setDailyInputs(dailyInputs);
    }

    private void copyMergingRequest(final MultipartFile mergingRequest, final Path taskInputPath) throws IOException {
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

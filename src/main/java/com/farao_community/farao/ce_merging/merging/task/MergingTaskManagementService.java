/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import brave.Tracer;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotRunException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.exception.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.request_metadata.RequestMetadataManager;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.mapper.MergingTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.SUCCESS;
import static java.nio.file.Files.createDirectories;
import static java.util.function.Predicate.not;
import static org.springframework.util.FileSystemUtils.copyRecursively;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

@Service
public class MergingTaskManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingTaskManagementService.class);

    private final CeMergingConfiguration configuration;
    private final MergingService mergingService;
    private final MergingTaskRepository taskRepository;
    private final MergingTaskMapper taskMapper;
    private final Tracer tracer;

    public MergingTaskManagementService(final CeMergingConfiguration configuration,
                                        final MergingService mergingService,
                                        final MergingTaskRepository taskRepository,
                                        final MergingTaskMapper taskMapper,
                                        final Tracer tracer) {
        this.configuration = configuration;
        this.mergingService = mergingService;
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
        this.tracer = tracer;
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        TASKS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public MergingTaskDto runTask(final long taskId) {
        final MergingTask task = getTaskById(taskId);
        return taskMapper.mergingTaskToMergingTaskDto(run(task));
    }

    public JsonApiDocument<MergingTaskDto> getTaskJsonDoc(final long taskId) {
        return JsonApiDocument.fromData(taskMapper.mergingTaskToMergingTaskDto(getTaskById(taskId)));
    }

    public MergingTaskDto createNewTask(final MultipartFile inputZip,
                                        final String inputRequestMetadata) {

        final MergingTask task = new MergingTask();
        // empty at this stage, but done to init id
        taskRepository.save(task);

        final String inputsDir = configuration.getInputsDirectoryPath(task);
        final RequestMetadataManager mgr = new RequestMetadataManager(inputsDir, inputRequestMetadata);

        try {
            final Path tmpInputPath = ZipUtils.unzipInputFileInTmp(inputZip);
            mgr.checkIfAllInputsAvailable(tmpInputPath);

            // create data in tmp folder then move it to the permanent one
            final Path inputsPath = Paths.get(inputsDir);
            createDirectories(inputsPath);
            createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
            createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
            copyRecursively(tmpInputPath, inputsPath);
            deleteRecursively(tmpInputPath);

            task.setArchiveFileOriginalName(inputZip.getOriginalFilename());
            mgr.feedTaskData(task);
            task.getInputs().setRealOffset(mgr.getRealRequestOffset());

            taskRepository.save(task);
            LOGGER.info("Merging task created with id: {}", task.getTaskId());
            return taskMapper.mergingTaskToMergingTaskDto(task);
        } catch (final IOException e) {
            LOGGER.error("Error during merging task creation.");
            taskRepository.delete(task);
            throw new ServiceIOException("Error during merging task creation", e);
        }

    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        ARTIFACTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    public SavedFile getXnodesInformation(final long taskId) {
        return getFinishedTaskById(taskId).getArtifacts().getXnodesInformationFile();
    }

    public SavedFile getCgmNetPositions(final long taskId) {
        return getFinishedTaskById(taskId).getArtifacts().getCgmNetPositionsFile();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getCgmOutput(final long taskId) {
        return getFinishedTaskById(taskId).getOutputs().getCgm();
    }

    public SavedFile getRefProgOutput(final long taskId) {
        return getFinishedTaskById(taskId).getOutputs().getRefProg();
    }

    public byte[] getOutputs(final long taskId) {
        final MergingTask task = getFinishedTaskById(taskId);
        return ZipUtils.zipDirectory(configuration.getOutputsDirectoryPath(task));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   INTERNAL (PRIVATE)
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private MergingTask run(final MergingTask task) {
        try {
            task.setRunTraceId(tracer.currentSpan().context().traceIdString());

            if (task.getTaskStatus() == RUNNING) {
                throw new TaskAlreadyRunningException(String.format("Task '%d' already running, could not be run again",
                                                                    task.getTaskId()));
            }
            task.setTaskStatus(RUNNING);

            LOGGER.info("Merging task {} is running.", task.getTaskId());
            taskRepository.save(task);

            LOGGER.info("Running merging task: '{}' ", task.getTaskId());
            mergingService.run(task);
            task.setTaskStatus(SUCCESS);
            taskRepository.save(task);
            LOGGER.info("Merging task: '{}' is finished with success", task.getTaskId());
            return task;

        } catch (final TaskAlreadyRunningException e) {
            throw e;
        } catch (final Exception e) {
            task.setStatusDetail(e.getMessage());
            task.setTaskStatus(ERROR);
            taskRepository.save(task);
            throw new CeMergingException(e.getMessage(), e);
        }

    }

    private MergingTask getTaskById(final long taskId) {
        final MergingTask task =
            taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Task %d not available",
                                                                               taskId)));
        handleDaylightSavingTime(task);
        return task;
    }

    private MergingTask getFinishedTaskById(final long taskId) throws ResourceNotRunException {
        final MergingTask task = getTaskById(taskId);
        if (task.getTaskStatus() == CREATED) {
            throw new ResourceNotRunException(String.format("Task %d has not been run",
                                                            taskId));
        } else if (task.getTaskStatus() == RUNNING) {
            throw new ResourceNotRunException(String.format("Task %d currently running",
                                                            taskId));
        }
        return task;
    }

    private void handleDaylightSavingTime(final MergingTask task) {
        // necessary treatment for the case of DST:
        // the changed hour (second 02:30 AM) will have an offset= +2 but really should be + 1
        final Inputs inputs = task.getInputs();
        final Optional<OffsetDateTime> taskDate = Optional.ofNullable(inputs.getTargetDate());
        final Optional<ZoneOffset> realOffset = Optional.ofNullable(inputs.getRealOffset());

        taskDate.ifPresent(date ->
                               realOffset.filter(not(date.getOffset()::equals))
                                   .ifPresent(offset ->
                                                  inputs.setTargetDate(OffsetDateTime.of(date.toLocalDateTime(),
                                                                                         offset))));
    }
}

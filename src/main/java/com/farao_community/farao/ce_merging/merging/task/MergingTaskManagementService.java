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
import com.farao_community.farao.ce_merging.common.exception.InvalidTaskException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotRunException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.exception.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.request_metadata.RequestMetadataManager;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
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

import static com.farao_community.farao.ce_merging.common.util.ZipUtils.unzipInputFileInTmp;
import static com.farao_community.farao.ce_merging.common.util.ZipUtils.zipDirectory;
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
        return taskMapper.mergingTaskToMergingTaskDto(run(getTaskById(taskId)));
    }

    public JsonApiDocument<MergingTaskDto> getTaskJsonDoc(final long taskId) {
        return JsonApiDocument.fromData(
            taskMapper.mergingTaskToMergingTaskDto(
                getTaskById(taskId)
            )
        );
    }

    public MergingTaskDto createNewTask(final MultipartFile inputZip,
                                        final String inputRequestMetadata) {

        final MergingTask task = new MergingTask();
        // empty at this stage, but done to init id
        taskRepository.save(task);

        final String inputsDir = configuration.getInputsDirectoryPath(task);
        final RequestMetadataManager requestMgr = new RequestMetadataManager(inputsDir, inputRequestMetadata);

        try {
            final Path tmpInputPath = unzipInputFileInTmp(inputZip);
            requestMgr.checkIfAllInputsAvailable(tmpInputPath);

            // create data in tmp folder then move it to the permanent one
            final Path inputsPath = Paths.get(inputsDir);
            createDirectories(inputsPath);
            createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
            createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));

            copyRecursively(tmpInputPath, inputsPath);
            deleteRecursively(tmpInputPath);

            task.setArchiveFileOriginalName(inputZip.getOriginalFilename());
            requestMgr.feedTaskData(task);
            task.getInputs().setRealOffset(requestMgr.getRealRequestOffset());

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
        return getArtifacts(taskId).getXnodesInformationFile();
    }

    public SavedFile getCgmNetPositions(final long taskId) {
        return getArtifacts(taskId).getCgmNetPositionsFile();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getCgm(final long taskId) {
        return getOutputs(taskId).getCgm();
    }

    public SavedFile getRefProg(final long taskId) {
        return getOutputs(taskId).getRefProg();
    }

    public byte[] getOutputZip(final long taskId) {
        return zipDirectory(
            configuration.getOutputsDirectoryPath(
                getFinishedTaskById(taskId)
            )
        );
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

        } catch (final TaskAlreadyRunningException alreadyRunningException) {
            throw alreadyRunningException;
        } catch (final Exception e) {
            final String error = e.getMessage();
            task.setStatusDetail(error);
            task.setTaskStatus(ERROR);
            taskRepository.save(task);
            throw new CeMergingException(error, e);
        }
    }

    private Artifacts getArtifacts(final long taskId) {
        return getFinishedTaskById(taskId).getArtifacts();
    }

    private Outputs getOutputs(final long taskId) {
        return getFinishedTaskById(taskId).getOutputs();
    }

    private MergingTask getTaskById(final long taskId) {
        final MergingTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException(String.format("Task %d not available",
                                                                           taskId)));
        handleDaylightSavingTime(task);
        return task;
    }

    private MergingTask getFinishedTaskById(final long taskId) throws ResourceNotRunException {
        final MergingTask task = getTaskById(taskId);

        return switch (task.getTaskStatus()) {
            case CREATED -> throw new ResourceNotRunException(String.format("Task %d has not been run",
                                                                            taskId));
            case RUNNING -> throw new ResourceNotRunException(String.format("Task %d currently running",
                                                                            taskId));
            case null -> throw new InvalidTaskException(String.format("Task %d has no status",
                                                                      taskId));
            case SUCCESS, ERROR -> task;
        };
    }

    private void handleDaylightSavingTime(final MergingTask task) {
        // necessary treatment in case of DST:
        // the changed hour (second 02:30 AM) will be UTC+2, but actually should be + 1
        final Inputs inputs = task.getInputs();
        final Optional<OffsetDateTime> taskDateOpt = Optional.ofNullable(inputs.getTargetDate());
        final Optional<ZoneOffset> realOffset = Optional.ofNullable(inputs.getRealOffset());

        taskDateOpt.ifPresent(taskDate ->
                                  realOffset.filter(not(taskDate.getOffset()::equals)) // if offsets are different ...
                                      .ifPresent(offset ->
                                                     inputs.setTargetDate(
                                                         OffsetDateTime.of(taskDate.toLocalDateTime(), offset)
                                                     ))); // ... we change the target date to have it at the real offset
    }
}

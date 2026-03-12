/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import brave.Tracer;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.ResourceNotRunException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.exception.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.merging.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.mapper.MergingTaskMapper;
import com.farao_community.farao.ce_merging.merging.request_metadata.RequestMetadataManager;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.RequestMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus.SUCCESS;
import static java.util.Optional.ofNullable;

@Slf4j
@AllArgsConstructor
@Service
public class MergingTaskManagementService {

    private final CeMergingConfiguration configuration;
    private final MergingService mergingService;
    private final MergingTaskRepository taskRepository;
    private final MergingTaskMapper taskMapper;
    private final Tracer tracer;

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        TASKS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public MergingTaskDto runTask(final long taskId) {
        MergingTask taskEntity = getTask(taskId);
        return taskMapper.mergingTaskToMergingTaskDto(run(taskEntity));
    }

    public JsonApiDocument<MergingTaskDto> getTaskJsonDoc(final long taskId) {
        return JsonApiDocument.fromData(taskMapper.mergingTaskToMergingTaskDto(getTask(taskId)));
    }

    public MergingTaskDto createNewTask(final MultipartFile inputZip,
                                        final String inputRequestMetadata) {
        final RequestMetadata requestMetadata;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            requestMetadata = mapper.readValue(inputRequestMetadata, RequestMetadata.class);
        } catch (final IOException e) {
            throw new ServiceIOException("Invalid request metadata", e);
        }

        final MergingTask task = new MergingTask();
        // empty at this stage, but done to init id
        taskRepository.save(task);

        try {
            final Path tmpInputPath = ZipUtils.unzipInputFileInTmp(inputZip);
            final String inputsDir = configuration.getInputsDirectoryPath(task);

            final RequestMetadataManager mgr = new RequestMetadataManager(inputsDir, requestMetadata);
            mgr.checkIfAllInputsAvailable(tmpInputPath);

            // create data in tmp folder then move it to the permanent one
            final Path inputsPath = Paths.get(inputsDir);
            Files.createDirectories(inputsPath);
            Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
            Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
            FileSystemUtils.copyRecursively(tmpInputPath, inputsPath);
            FileSystemUtils.deleteRecursively(tmpInputPath);

            task.setArchiveFileOriginalName(inputZip.getOriginalFilename());
            mgr.feedTaskData(task);
            task.getInputs().setRealOffset(mgr.getRealRequestOffset());

            taskRepository.save(task);
            log.info("Merging task created with id: {}", task.getTaskId());
            return taskMapper.mergingTaskToMergingTaskDto(task);
        } catch (final IOException e) {
            log.error("Error during merging task creation.");
            taskRepository.delete(task);
            throw new ServiceIOException("Error during merging task creation", e);
        }

    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        ARTIFACTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    public SavedFile getXnodesInformation(final long taskId) {
        return getTaskIfRun(taskId).getArtifacts().getXnodesInformationFile();
    }

    public SavedFile getCgmNetPositions(final long taskId) {
        return getTaskIfRun(taskId).getArtifacts().getCgmNetPositionsFile();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getCgmOutput(final long taskId) {
        return getTaskIfRun(taskId).getOutputs().getCgm();
    }

    public SavedFile getRefProgOutput(final long taskId) {
        return getTaskIfRun(taskId).getOutputs().getRefProg();
    }

    public byte[] getOutputs(final long taskId) {
        final MergingTask task = getTaskIfRun(taskId);
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

            log.info("Merging task {} is running.", task.getTaskId());
            taskRepository.save(task);

            log.info("Running merging task: '{}' ", task.getTaskId());
            mergingService.run(task);
            task.setTaskStatus(SUCCESS);
            taskRepository.save(task);
            log.info("Merging task: '{}' is finished with success", task.getTaskId());
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

    private MergingTask getTask(final long taskId) {
        final MergingTask task =
            taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Task %d not available",
                                                                               taskId)));
        handleDaylightSavingTime(task);
        return task;
    }

    private MergingTask getTaskIfRun(final long taskId) throws ResourceNotRunException {
        final MergingTask task = getTask(taskId);
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
        ofNullable(inputs.getTargetDate())
            .ifPresent(targetDate ->
                           ofNullable(inputs.getRealOffset())
                               .filter(targetDate.getOffset()::equals)
                               .ifPresent(offset ->
                                              inputs.setTargetDate(OffsetDateTime.of(targetDate.toLocalDateTime(),
                                                                                     offset))));
    }
}

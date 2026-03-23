/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus;
import com.farao_community.farao.ce_merging.merging.task.mapper.MergingTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.SUCCESS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.INPUTS;
import static test_utils.CeTestUtils.INPUTS_ZIP_NAME;
import static test_utils.CeTestUtils.METADATA;
import static test_utils.CeTestUtils.MIME_ZIP;
import static test_utils.CeTestUtils.anyTask;
import static test_utils.CeTestUtils.byteContentOf;
import static test_utils.CeTestUtils.stringContentOf;
import static test_utils.CeTestUtils.taskDtoWithIdAndStatus;
import static test_utils.CeTestUtils.taskWithIdAndStatus;
import static test_utils.assertions.CeTaskAssert.assertThat;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

@SpringBootTest
@TestConfiguration
class MergingTaskManagementServiceTest {

    private static final long ONE = 1L;

    @Autowired
    CeMergingConfiguration ceMergingConfiguration;

    private final MergingService mergingService = mock(MergingService.class);
    private final MergingTaskRepository taskRepository = mock(MergingTaskRepository.class);
    private final MergingTaskMapper taskMapper = mock(MergingTaskMapper.class);

    MergingTaskManagementService service;

    @BeforeEach
    void setUp() {
        // to obtain configuration from autowire
        service = new MergingTaskManagementService(ceMergingConfiguration,
                                                   mergingService,
                                                   taskRepository,
                                                   taskMapper);
    }

    @Test
    void shouldCreateTask() {
        when(taskRepository.save(anyTask()))
            .thenReturn(taskWithIdAndStatus(1L, CREATED));

        final MockMultipartFile zipFile = new MockMultipartFile(INPUTS_ZIP_NAME,
                                                                INPUTS_ZIP_NAME,
                                                                MIME_ZIP,
                                                                byteContentOf(INPUTS));

        service.createNewTask(zipFile, stringContentOf(METADATA));

        verify(taskRepository, times(2))
            .save(anyTask());
        verify(taskMapper)
            .mergingTaskToMergingTaskDto(anyTask());
    }

    @Test
    void shouldCatchExceptionInCreation() {
        when(taskRepository.save(anyTask()))
            .thenReturn(taskWithIdAndStatus(1L, CREATED));

        final MockMultipartFile zipFile = new MockMultipartFile(INPUTS_ZIP_NAME,
                                                                INPUTS_ZIP_NAME,
                                                                "application/zip",
                                                                byteContentOf(METADATA));

        assertThatThrownBy(() -> service.createNewTask(zipFile, stringContentOf(METADATA)))
            .isServiceException();

        verify(taskRepository)
            .delete(anyTask());
    }

    @Test
    void shouldRunTask() {

        final MergingTask task = taskWithIdAndStatus(ONE, CREATED);

        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(task));

        service.runTask(ONE);

        verify(mergingService)
            .run(anyTask());
        verify(taskMapper)
            .mergingTaskToMergingTaskDto(anyTask());
        verify(taskRepository, times(2))
            .save(anyTask());

        assertThat(task).hasStatus(SUCCESS);
    }

    @Test
    void shouldThrowIfTaskAlreadyRunning() {
        final MergingTask runningTask = taskWithIdAndStatus(ONE, RUNNING);
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.runTask(ONE))
            .isTaskException()
            .hasMessage("Task 1 already running, could not be run again");

        assertThat(runningTask).hasStatus(RUNNING);
    }

    @Test
    void shouldThrowIfTaskNotFound() {
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runTask(ONE))
            .isTaskException()
            .hasMessage("Task 1 not available");
    }

    @Test
    void shouldChangeStatusToErrorIfExceptionThrown() {
        final MergingTask failingTask = taskWithIdAndStatus(ONE, CREATED);
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(failingTask));

        doThrow(new TaskNotValidException("test"))
            .when(mergingService)
            .run(anyTask());

        assertThatThrownBy(() -> service.runTask(ONE))
            .isServiceException();

        assertThat(failingTask).hasStatus(ERROR);
    }

    @Test
    void shouldGetTaskJsonDoc() {
        final MergingTask task = taskWithIdAndStatus(ONE, CREATED);
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(task));
        when(taskMapper.mergingTaskToMergingTaskDto(anyTask())).thenReturn(taskDtoWithIdAndStatus(ONE, CREATED));

        assertThat(task)
            .isSameTaskAs(service.getTaskJsonDoc(ONE)
                              .data
                              .getFirst());
    }

    private static final List<TaskStatus> FINISHED_STATUSES = List.of(SUCCESS, ERROR);
    private static final List<TaskStatus> NOT_FINISHED_STATUSES = new ArrayList<>();

    static {
        //because List.of doesn't accept null
        NOT_FINISHED_STATUSES.add(CREATED);
        NOT_FINISHED_STATUSES.add(RUNNING);
        NOT_FINISHED_STATUSES.add(null);
    }

    @ParameterizedTest
    @FieldSource("FINISHED_STATUSES")
    void shouldGetTaskFilesIfTaskFinished(final TaskStatus status) {
        final MergingTask task = taskWithIdAndStatus(ONE, status);
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(task));

        service.getCgm(ONE);
        service.getCgmNetPositions(ONE);
        service.getRefProg(ONE);
        service.getXnodesInformation(ONE);

        verify(taskRepository, times(4))
            .findById(ONE);
    }

    @ParameterizedTest
    @FieldSource("NOT_FINISHED_STATUSES")
    void shouldNotGetTaskFilesIfTaskNotFinished(final TaskStatus status) {
        final MergingTask task = taskWithIdAndStatus(ONE, status);
        when(taskRepository.findById(ONE))
            .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.getCgm(ONE))
            .isTaskException()
            .hasMessageContaining("Task 1");
    }

    @Test
    void shouldGetOutputZip() {
        try (final MockedStatic<ZipUtils> zipUtils = mockStatic(ZipUtils.class)) {

            final MergingTask task = taskWithIdAndStatus(ONE, SUCCESS);
            when(taskRepository.findById(ONE))
                .thenReturn(Optional.of(task));

            zipUtils.when(() -> ZipUtils.zipDirectory(anyString()))
                .thenReturn("TEST".getBytes(UTF_8));
            final byte[] response = service.getOutputZip(ONE);

            zipUtils.verify(() -> ZipUtils.zipDirectory(anyString()));
            assertEquals("TEST", new String(response));

        }
    }
}

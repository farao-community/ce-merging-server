/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.util.ZipUtils;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.enums.TaskStatus;
import com.farao_community.farao.ce_merging.merging.task.mapper.MergingTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.SUCCESS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.ID_1;
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
@ExtendWith(OutputCaptureExtension.class)
class MergingTaskManagementServiceTest {
    @Autowired
    CeMergingConfiguration ceMergingConfiguration;

    private final MergingService mergingService = mock(MergingService.class);
    private final MergingTaskRepository repository = mock(MergingTaskRepository.class);
    private final MergingTaskMapper mapper = mock(MergingTaskMapper.class);

    MergingTaskManagementService service;

    @BeforeEach
    void setUp() {
        // to obtain configuration from autowire
        service = new MergingTaskManagementService(ceMergingConfiguration,
                                                   mergingService,
                                                   repository,
                                                   mapper);
    }

    @Test
    void shouldCreateTask() {
        when(repository.save(anyTask()))
            .thenReturn(taskWithIdAndStatus(ID_1, CREATED));

        service.createNewTask(mockZip(INPUTS), stringContentOf(METADATA));

        verify(repository, times(2))
            .save(anyTask());
        verify(mapper)
            .mergingTaskToMergingTaskDto(anyTask());
    }

    @Test
    void shouldCatchExceptionInCreation() {
        when(repository.save(anyTask()))
            .thenReturn(taskWithIdAndStatus(ID_1, CREATED));

        assertThatThrownBy(() -> service.createNewTask(mockZip(METADATA), stringContentOf(METADATA)))
            .isValidServiceException();

        verify(repository)
            .delete(anyTask());
    }

    @Test
    void shouldRunTask() {

        final MergingTask task = taskWithIdAndStatus(ID_1, CREATED);

        when(repository.findById(ID_1))
            .thenReturn(Optional.of(task));

        service.runTask(ID_1);

        verify(mergingService)
            .run(anyTask());
        verify(mapper)
            .mergingTaskToMergingTaskDto(anyTask());
        verify(repository, times(2))
            .save(anyTask());

        assertThat(task).hasStatus(SUCCESS);
    }

    @Test
    void shouldThrowIfTaskAlreadyRunning() {
        final MergingTask runningTask = taskWithIdAndStatus(ID_1, RUNNING);
        when(repository.findById(ID_1))
            .thenReturn(Optional.of(runningTask));

        assertThatThrownBy(() -> service.runTask(ID_1))
            .isTaskException()
            .hasMessage("Task 1 already running, could not be run again");

        assertThat(runningTask).hasStatus(RUNNING);
    }

    @Test
    void shouldThrowIfTaskNotFound() {
        when(repository.findById(ID_1))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runTask(ID_1))
            .isTaskException()
            .hasMessage("Task 1 not available");
    }

    @Test
    void shouldChangeStatusToErrorIfExceptionThrown() {
        final MergingTask failingTask = taskWithIdAndStatus(ID_1, CREATED);
        when(repository.findById(ID_1))
            .thenReturn(Optional.of(failingTask));

        doThrow(new TaskNotValidException("test"))
            .when(mergingService)
            .run(anyTask());

        assertThatThrownBy(() -> service.runTask(ID_1))
            .isValidServiceException();

        assertThat(failingTask).hasStatus(ERROR);
    }

    @Test
    void shouldGetTaskJsonDoc() {
        final MergingTask task = taskWithIdAndStatus(ID_1, CREATED);
        when(repository.findById(ID_1))
            .thenReturn(Optional.of(task));
        when(mapper.mergingTaskToMergingTaskDto(anyTask())).thenReturn(taskDtoWithIdAndStatus(ID_1, CREATED));

        assertThat(task)
            .isSameTaskAs(service.getTaskJsonDoc(ID_1).data.getFirst());
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
        final MergingTask task = taskWithIdAndStatus(ID_1, status);
        when(repository.findById(ID_1))
            .thenReturn(Optional.of(task));

        service.getCgm(ID_1);
        service.getCgmNetPositions(ID_1);
        service.getRefProg(ID_1);
        service.getXnodesInformation(ID_1);

        verify(repository, times(4))
            .findById(ID_1);
    }

    @ParameterizedTest
    @FieldSource("NOT_FINISHED_STATUSES")
    void shouldNotGetTaskFilesIfTaskNotFinished(final TaskStatus status) {
        final MergingTask task = taskWithIdAndStatus(ID_1, status);
        when(repository.findById(ID_1))
            .thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.getCgm(ID_1))
            .isTaskException()
            .hasMessageContaining("Task 1");
    }

    @Test
    void shouldGetOutputZip() {
        try (final MockedStatic<ZipUtils> zipUtils = mockStatic(ZipUtils.class)) {

            final MergingTask task = taskWithIdAndStatus(ID_1, SUCCESS);
            when(repository.findById(ID_1))
                .thenReturn(Optional.of(task));

            zipUtils.when(() -> ZipUtils.zipDirectory(anyString()))
                .thenReturn("TEST".getBytes(UTF_8));
            final byte[] response = service.getOutputZip(ID_1);

            zipUtils.verify(() -> ZipUtils.zipDirectory(anyString()));
            assertEquals("TEST", new String(response));

        }
    }

    private MockMultipartFile mockZip(final String path) {
        return new MockMultipartFile(INPUTS_ZIP_NAME,
                                     INPUTS_ZIP_NAME,
                                     MIME_ZIP,
                                     byteContentOf(path));
    }
}

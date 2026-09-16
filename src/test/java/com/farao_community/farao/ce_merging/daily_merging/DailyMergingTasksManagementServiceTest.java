/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.task.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyMergingTasksManagementServiceTest {

    private static final long TASK_ID_1 = 1L;
    private static final long TASK_ID_2 = 2L;
    private static final int MERGING_VERSION = 2;

    @TempDir
    Path tempDir;

    private CeMergingConfiguration configuration;
    private DailyMergingRepository repository;
    private MergingTaskManagementService service;
    private DailyMergingService dailyMergingService;
    private DailyMergingTasksManagementService managementService;

    @BeforeEach
    void setUp() {
        configuration = mock(CeMergingConfiguration.class);
        repository = mock(DailyMergingRepository.class);
        service = mock(MergingTaskManagementService.class);
        dailyMergingService = mock(DailyMergingService.class);
        managementService = new DailyMergingTasksManagementService(
                configuration,
                repository,
                service,
                dailyMergingService
        );
    }

    @Test
    void shouldGetTaskById() {
        final DailyMergingTask task = dailyTask(TASK_ID_1);
        givenTaskFound(task);
        final DailyMergingTask result = managementService.getTaskById(TASK_ID_1);
        assertSame(task, result);
        verify(repository).findById(TASK_ID_1);
    }

    @Test
    void shouldThrowExceptionWhenTaskDoesNotExist() {
        givenTaskNotFound();
        assertThrows(
                TaskNotFoundException.class,
                () -> managementService.getTaskById(TASK_ID_1)
        );
        verify(repository).findById(TASK_ID_1);
    }

    @Test
    void shouldGetTask() {
        final DailyMergingTask task = dailyTask(TASK_ID_1);
        givenTaskFound(task);
        final DailyMergingTask result = managementService.getTask(TASK_ID_1);
        assertSame(task, result);
    }

    @Test
    void shouldGetAllTasks() {
        final DailyMergingTask task1 = dailyTask(TASK_ID_1);
        final DailyMergingTask task2 = dailyTask(TASK_ID_2);
        when(repository.findAll()).thenReturn(List.of(task1, task2));
        final List<DailyMergingTask> result = managementService.getAllTasks();
        assertEquals(List.of(task1, task2), result);
    }

    @Test
    void shouldDeleteTask() throws Exception {
        final DailyMergingTask task = dailyTask(TASK_ID_1);
        final Path taskDirectory = tempDir.resolve("daily-task-" + TASK_ID_1);
        Files.createDirectories(taskDirectory);
        Files.writeString(taskDirectory.resolve("output.txt"), "test");
        givenTaskFound(task);
        when(configuration.getDailyDirectoryPath(task)).thenReturn(taskDirectory.toString());
        managementService.deleteTask(TASK_ID_1);
        assertFalse(Files.exists(taskDirectory));
        verify(repository).deleteById(TASK_ID_1);
    }

    @Test
    void shouldDeleteAllTasks() {
        final DailyMergingTask task1 = dailyTask(TASK_ID_1);
        final DailyMergingTask task2 = dailyTask(TASK_ID_2);
        when(repository.findAll()).thenReturn(List.of(task1, task2));
        givenTaskFound(task1);
        givenTaskFound(task2);
        when(configuration.getDailyDirectoryPath(task1)).thenReturn(tempDir.resolve("task-1").toString());
        when(configuration.getDailyDirectoryPath(task2)).thenReturn(tempDir.resolve("task-2").toString());
        managementService.deleteAllTasks();
        verify(repository).deleteById(TASK_ID_1);
        verify(repository).deleteById(TASK_ID_2);
    }

    @Test
    void shouldGetMergingRequest() {
        final DailyMergingTask task = dailyTask(TASK_ID_1);
        final SavedFile mergingRequest = new SavedFile();
        final DailyInputs inputs = new DailyInputs();

        inputs.setMergingRequest(mergingRequest);
        task.setDailyInputs(inputs);

        givenTaskFound(task);

        final SavedFile result = managementService.getMergingRequest(TASK_ID_1);

        assertSame(mergingRequest, result);
    }

    @Test
    void shouldCreateDailyTask() throws Exception {
        final MergingTask mergingTask = mockSuccessfulMergingTask();
        givenDailyTaskPaths();

        final MockMultipartFile mergingRequest = new MockMultipartFile(
                "mergingRequest",
                "merging-request.json",
                "application/json",
                "{\"test\":true}".getBytes()
        );
        final DailyMergingTask dailyTask = managementService.createDailyTask(List.of(TASK_ID_1), mergingRequest, MERGING_VERSION);
        assertNotNull(dailyTask);
        assertEquals(MERGING_VERSION, dailyTask.getVersion());
        assertEquals(List.of(TASK_ID_1), dailyTask.getMergingTaskIds());
        final Path inputFile = tempDir
                .resolve("inputs")
                .resolve("merging-request.json");

        assertTrue(Files.exists(inputFile));
        assertEquals("{\"test\":true}", Files.readString(inputFile));
        verify(repository, atLeast(1)).save(any(DailyMergingTask.class));
        verify(service).checkTaskExist(TASK_ID_1);
        verify(service).getTaskById(TASK_ID_1);
    }

    @Test
    void shouldRunDailyMergingTaskSuccessfully() {
        final DailyMergingTask dailyTask = dailyTask(TASK_ID_1, TaskStatus.CREATED, List.of(TASK_ID_1, TASK_ID_2));
        final MergingTask task1 = mock(MergingTask.class);
        final MergingTask task2 = mock(MergingTask.class);
        givenTaskFound(dailyTask);
        when(service.getTaskById(TASK_ID_1)).thenReturn(task1);
        when(service.getTaskById(TASK_ID_2)).thenReturn(task2);
        final DailyMergingTask task = managementService.runDailyMergingTask(TASK_ID_1);
        assertSame(dailyTask, task);
        assertEquals(TaskStatus.SUCCESS, task.getTaskStatus());
        verify(dailyMergingService).run(eq(dailyTask), eq(List.of(task1, task2)));
        verify(repository, atLeast(2)).save(dailyTask);
    }

    @Test
    void shouldSetTaskToErrorWhenDailyMergingFails() {
        final DailyMergingTask dailyTask = dailyTask(TASK_ID_1, TaskStatus.CREATED, List.of(TASK_ID_2));
        final MergingTask mergingTask = mock(MergingTask.class);
        final RuntimeException exception = new RuntimeException("Computation failed");

        givenTaskFound(dailyTask);
        when(service.getTaskById(TASK_ID_2)).thenReturn(mergingTask);
        doThrow(exception)
                .when(dailyMergingService)
                .run(eq(dailyTask), eq(List.of(mergingTask)));

        final RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> managementService.runDailyMergingTask(TASK_ID_1)
        );
        assertSame(exception, thrown);
        assertEquals(TaskStatus.ERROR, dailyTask.getTaskStatus());
        verify(repository, atLeast(2)).save(dailyTask);
    }

    @Test
    void shouldNotRunTaskAlreadyRunning() {
        final DailyMergingTask dailyTask = dailyTask(TASK_ID_1, TaskStatus.RUNNING, List.of());
        givenTaskFound(dailyTask);
        assertThrows(
                TaskAlreadyRunningException.class,
                () -> managementService.runDailyMergingTask(TASK_ID_1)
        );
        verify(dailyMergingService, never()).run(any(), any());
    }

    private DailyMergingTask dailyTask(final long id) {
        return dailyTask(id, TaskStatus.CREATED, List.of());
    }

    private DailyMergingTask dailyTask(final long id, final TaskStatus status, final List<Long> mergingTaskIds) {
        final DailyMergingTask task = new DailyMergingTask();
        task.setId(id);
        task.setTaskStatus(status);
        task.setMergingTaskIds(mergingTaskIds);
        return task;
    }

    private void givenTaskFound(final DailyMergingTask task) {
        when(repository.findById(task.getId())).thenReturn(Optional.of(task));
    }

    private void givenTaskNotFound() {
        when(repository.findById(TASK_ID_1)).thenReturn(Optional.empty());
    }

    private MergingTask mockSuccessfulMergingTask() {
        final MergingTask mergingTask = mock(MergingTask.class);
        when(mergingTask.getStatus()).thenReturn(TaskStatus.SUCCESS);
        when(service.checkTaskExist(TASK_ID_1)).thenReturn(true);
        when(service.getTaskById(TASK_ID_1)).thenReturn(mergingTask);

        return mergingTask;
    }

    private void givenDailyTaskPaths() {
        when(configuration.getDailyInputsDirectoryPath(any(DailyMergingTask.class))).thenReturn(tempDir.resolve("inputs").toString());
        when(configuration.getDailyOutputsDirectoryPath(any(DailyMergingTask.class))).thenReturn(tempDir.resolve("outputs").toString());
        when(configuration.getDailyDirectoryPath(any(DailyMergingTask.class))).thenReturn(tempDir.toString());
    }
}

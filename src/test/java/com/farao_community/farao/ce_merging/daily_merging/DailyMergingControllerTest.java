/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyMergingControllerTest {

    private static final long TASK_ID = 42L;

    @Mock
    private DailyMergingTasksManagementService dailyTasksManagementService;

    @TempDir
    Path tempDir;

    private DailyMergingController controller;

    @BeforeEach
    void setUp() {
        controller = new DailyMergingController(dailyTasksManagementService);
    }

    @Test
    void shouldCreateDailyTask() {
        final int mergingVersion = 3;
        final Long[] taskIds = {1L, 2L, 3L};
        final MultipartFile mergingRequest = new MockMultipartFile(
                "mergingRequest",
                "request.json",
                "application/json",
                "{}".getBytes()
        );
        final DailyMergingTask task = mock(DailyMergingTask.class);
        when(task.getId()).thenReturn(TASK_ID);
        when(dailyTasksManagementService.createDailyTask(any(), same(mergingRequest), eq(mergingVersion))).thenReturn(task);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        final ResponseEntity<DailyMergingTask> response = controller.createDailyTask(taskIds, mergingRequest, mergingVersion);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(task, response.getBody());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().endsWith("/daily-merging/tasks/" + TASK_ID));
        verify(dailyTasksManagementService).createDailyTask(
                List.of(1L, 2L, 3L),
                mergingRequest,
                mergingVersion
        );
    }

    @Test
    void shouldRunTask() {
        final DailyMergingTask task = mock(DailyMergingTask.class);
        when(dailyTasksManagementService.runDailyMergingTask(TASK_ID)).thenReturn(task);
        final ResponseEntity<DailyMergingTask> response = controller.runTask(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(task, response.getBody());
        verify(dailyTasksManagementService).runDailyMergingTask(TASK_ID);
    }

    @Test
    void shouldListTasks() {
        final DailyMergingTask task1 = mock(DailyMergingTask.class);
        final DailyMergingTask task2 = mock(DailyMergingTask.class);
        final List<DailyMergingTask> tasks = List.of(task1, task2);
        when(dailyTasksManagementService.getAllTasks()).thenReturn(tasks);
        final ResponseEntity<List<DailyMergingTask>> response = controller.listTasks();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(tasks, response.getBody());
        verify(dailyTasksManagementService).getAllTasks();
    }

    @Test
    void shouldGetTask() {
        final DailyMergingTask task = mock(DailyMergingTask.class);
        when(dailyTasksManagementService.getTask(TASK_ID)).thenReturn(task);
        final ResponseEntity<DailyMergingTask> response = controller.getTask(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(task, response.getBody());
        verify(dailyTasksManagementService).getTask(TASK_ID);
    }

    @Test
    void shouldDeleteTask() {
        final ResponseEntity<Void> response = controller.deleteTask(TASK_ID);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(dailyTasksManagementService).deleteTask(TASK_ID);
    }

    @Test
    void shouldDeleteAllTasks() {
        final ResponseEntity<Void> response = controller.deleteAllTasks();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(dailyTasksManagementService).deleteAllTasks();
    }

    @Test
    void shouldGetMergingRequest() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getMergingRequest(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getMergingRequest(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getMergingRequest(TASK_ID);
    }

    @Test
    void shouldGetDailyResultPackage() {
        final byte[] content = "zip content".getBytes();
        when(dailyTasksManagementService.getDailyResultPackage(TASK_ID)).thenReturn(content);
        final ResponseEntity<byte[]> response = controller.getDailyResultPackage(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(content, response.getBody());
        verify(dailyTasksManagementService).getDailyResultPackage(TASK_ID);
    }

    @Test
    void shouldGetDailyMergingResponse() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getDailyMergingResponse(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getDailyMergingResponse(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getDailyMergingResponse(TASK_ID);
    }

    @Test
    void shouldGetDailyMergingLogs() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getDailyMergingLogs(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getDailyMergingLogs(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getDailyMergingLogs(TASK_ID);
    }

    @Test
    void shouldGetDailyCgmZip() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getDailyCgmZip(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getDailyCgmZip(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getDailyCgmZip(TASK_ID);
    }

    @Test
    void shouldGetDailyGlskQualityReport() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getDailyGlskReport(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getDailyGlskQualityReport(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getDailyGlskReport(TASK_ID);
    }

    @Test
    void shouldGetMergingReport() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getMergingReport(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getMergingReport(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getMergingReport(TASK_ID);
    }

    @Test
    void shouldGetDailyRefProgOutput() throws IOException {
        final SavedFile savedFile = savedFile();
        when(dailyTasksManagementService.getDailyRefProgOutput(TASK_ID)).thenReturn(savedFile);
        final ResponseEntity<byte[]> response = controller.getDailyRefProgOutput(TASK_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(dailyTasksManagementService).getDailyRefProgOutput(TASK_ID);
    }

    private SavedFile savedFile() throws IOException {
        final Path file = Files.createTempFile(tempDir, "test-", ".tmp");
        Files.write(file, "mock content".getBytes());
        return new SavedFile(
                "fileName",
                file.toString(),
                "mock"
        );
    }
}

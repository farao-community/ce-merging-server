/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.TraceContext;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.mapper.MergingTaskMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.anyTask;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.byteContentOfTestFile;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringContentOfTestFile;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestConfiguration
class MergingTaskManagementServiceTest {

    private static final String INPUTS = "request-metadata/inputs/";
    private static final String METADATA = "request-metadata/metadata.json";
    private static final String ZIP_NAME = "inputs.zip";
    private static final long ID = 1L;

    @InjectMocks
    CeMergingConfiguration ceMergingConfiguration;

    private final MergingService mergingService = Mockito.mock(MergingService.class);
    private final MergingTaskRepository taskRepository = Mockito.mock(MergingTaskRepository.class);
    private final MergingTaskMapper taskMapper = Mockito.mock(MergingTaskMapper.class);
    Tracer tracer;

    MergingTaskManagementService service;

    @BeforeEach
    void setUp() {
        tracer = Tracing.newBuilder().build().tracer();
        final TraceContext ctx = TraceContext.newBuilder().traceId(ID).spanId(ID).build();
        final Span span = tracer.toSpan(ctx);
        tracer.withSpanInScope(span);

        // to obtain configuration from autowire
        service = new MergingTaskManagementService(ceMergingConfiguration,
                                                   mergingService,
                                                   taskRepository,
                                                   taskMapper,
                                                   tracer);
    }

    @Test
    void shouldCreateTask() {
        final MergingTask task = new MergingTask();
        task.setTaskId(1);
        task.setArchiveFileOriginalName(ZIP_NAME);
        Mockito.when(taskRepository.save(any(MergingTask.class))).thenReturn(task);

        final MockMultipartFile zipFile = new MockMultipartFile(ZIP_NAME,
                                                                ZIP_NAME,
                                                                "application/zip",
                                                                byteContentOfTestFile(INPUTS + ZIP_NAME));
        service.createNewTask(zipFile, stringContentOfTestFile(METADATA));
        verify(taskRepository, times(2))
            .save(anyTask());
        verify(taskMapper)
            .mergingTaskToMergingTaskDto(anyTask());
    }

    @Test
    void shouldRunTask() {
        final MergingTask task = new MergingTask();
        task.setTaskId(ID);
        task.setArchiveFileOriginalName(ZIP_NAME);
        task.setTaskStatus(CREATED);
        Mockito.when(taskRepository.findById(ID)).thenReturn(Optional.of(task));

        service.runTask(ID);

        verify(mergingService).run(anyTask());
        verify(taskMapper).mergingTaskToMergingTaskDto(anyTask());
        verify(taskRepository, times(2)).save(anyTask());

        assertThat(task.getTaskStatus()).isEqualTo(SUCCESS);

    }

    @Test
    void shouldThrowIfTaskAlreadyRunning() {
        final MergingTask task = new MergingTask();
        task.setTaskId(ID);
        task.setArchiveFileOriginalName(ZIP_NAME);
        task.setTaskStatus(RUNNING);
        Mockito.when(taskRepository.findById(ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> service.runTask(ID))
            .hasMessage("Task '1' already running, could not be run again");

    }
}

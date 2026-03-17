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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.anyTask;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.byteContentOf;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringContentOf;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.withIdAndStatus;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestConfiguration
class MergingTaskManagementServiceTest {

    private static final String INPUTS = "request-metadata/inputs/";
    private static final String METADATA = "request-metadata/metadata.json";
    private static final String INPUTS_ZIP_NAME = "inputs.zip";
    private static final long ID = 1L;

    @Autowired
    CeMergingConfiguration ceMergingConfiguration;

    private final MergingService mergingService = Mockito.mock(MergingService.class);
    private final MergingTaskRepository taskRepository = Mockito.mock(MergingTaskRepository.class);
    private final MergingTaskMapper taskMapper = Mockito.mock(MergingTaskMapper.class);
    Tracer tracer;

    MergingTaskManagementService service;

    @BeforeEach
    void setUp() {
        tracer = Tracing
            .newBuilder()
            .build()
            .tracer();

        final TraceContext ctx = TraceContext
            .newBuilder()
            .traceId(10L)
            .spanId(10L)
            .build();

        final Span span = tracer.toSpan(ctx);
        tracer.withSpanInScope(span);

        // to obtain configuration from autowire, and instantiate tracer
        service = new MergingTaskManagementService(ceMergingConfiguration,
                                                   mergingService,
                                                   taskRepository,
                                                   taskMapper,
                                                   tracer);
    }

    @Test
    void shouldCreateTask() {
        when(taskRepository.save(anyTask()))
            .thenReturn(withIdAndStatus(1L, CREATED));

        final MockMultipartFile zipFile = new MockMultipartFile(INPUTS_ZIP_NAME,
                                                                INPUTS + INPUTS_ZIP_NAME,
                                                                "application/zip",
                                                                byteContentOf(INPUTS + INPUTS_ZIP_NAME));

        service.createNewTask(zipFile, stringContentOf(METADATA));

        verify(taskRepository, times(2))
            .save(anyTask());
        verify(taskMapper)
            .mergingTaskToMergingTaskDto(anyTask());
    }

    @Test
    void shouldRunTask() {

        final MergingTask task = withIdAndStatus(ID, CREATED);

        when(taskRepository.findById(ID))
            .thenReturn(Optional.of(task));

        service.runTask(ID);

        verify(mergingService)
            .run(anyTask());
        verify(taskMapper)
            .mergingTaskToMergingTaskDto(anyTask());
        verify(taskRepository, times(2))
            .save(anyTask());

        assertThat(task.getTaskStatus())
            .isEqualTo(SUCCESS);

    }

    @Test
    void shouldThrowIfTaskAlreadyRunning() {
        when(taskRepository.findById(ID))
            .thenReturn(Optional.of(withIdAndStatus(ID, RUNNING)));

        assertThatThrownBy(() -> service.runTask(ID))
            .hasMessage("Task '1' already running, could not be run again");

    }
}

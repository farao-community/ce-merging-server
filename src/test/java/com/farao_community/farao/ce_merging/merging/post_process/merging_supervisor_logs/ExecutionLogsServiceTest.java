/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs;

import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity.LoggingEvent;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity.LoggingEventProperty;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
class ExecutionLogsServiceTest {
    private static final String LONG_FORMATTED_MESSAGE = "Long formatted message giving full details from log";
    private static final String WARNING = "WARNING";

    private static final long TIMESTMP = 12L;
    private static final long EVENT_ID = 23L;

    @Autowired
    ExecutionLogsService executionLogsService;

    @Mock
    LoggingEventRepository repository;

    LoggingEvent setupModel() {
        LoggingEvent log = new LoggingEvent();
        log.setTimestmp(TIMESTMP);
        log.setEventId(EVENT_ID);
        log.setFormattedMessage(LONG_FORMATTED_MESSAGE);
        log.setLevelString(WARNING);

        LoggingEventProperty property1 = new LoggingEventProperty();
        property1.setLoggingEvent(log);
        property1.setMappedKey("taskId");
        property1.setMappedValue("152");

        LoggingEventProperty property2 = new LoggingEventProperty();
        property2.setLoggingEvent(log);
        property2.setMappedKey("merging-step");
        property2.setMappedValue("OPEN_LOAD_FLOW_LOGS");
        Set<LoggingEventProperty> properties = new HashSet<>();
        properties.add(property1);
        properties.add(property2);
        log.setProperties(properties);
        return log;
    }

    @Test
    public void testLogsModelConversion() {
        MergingTask task = new MergingTask();
        task.setId(152L);
        Set<LoggingEvent> logs = Set.of(setupModel());
        Mockito.when(repository.findLogsByTaskId(eq(EVENT_ID))).thenReturn(logs);
        byte[] output = executionLogsService.generateLogsForMergingSupervisor(task);
        Assertions.assertThat(output).isNotEmpty();
    }

}


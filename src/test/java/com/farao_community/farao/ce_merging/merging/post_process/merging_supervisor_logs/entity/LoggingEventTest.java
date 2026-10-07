/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingEventTest {

    private static final long TIMESTMP = 12L;
    private static final long EVENT_ID = 23L;
    private static final String ARG_0 = "Arg0";
    private static final String ARG_1 = "Arg1";
    private static final String ARG_2 = "Arg2";
    private static final String ARG_3 = "Arg3";
    private static final String CALLER_CLASS_NAME = "Caller Class Name";
    private static final String LONG_FORMATTED_MESSAGE = "Long formatted message giving full details from log";
    private static final String CALLER_LINE = "123";
    private static final String METHOD_NAME_CALLER = "Method name caller";
    private static final String WARNING = "WARNING";
    private static final int REFERENCE_FLAG = 3;
    private static final String THREAD_NAME = "ThreadName";
    private static final String LOGGER_NAME = "Logger Name";
    private static final String CALLER_FILENAME = "Caller Filename";

    @Test
    void testSettersAndGetters() {
        LoggingEvent log = new LoggingEvent();
        log.setTimestmp(TIMESTMP);
        log.setEventId(EVENT_ID);
        log.setArg0(ARG_0);
        log.setArg1(ARG_1);
        log.setArg2(ARG_2);
        log.setArg3(ARG_3);
        log.setCallerClass(CALLER_CLASS_NAME);
        log.setFormattedMessage(LONG_FORMATTED_MESSAGE);
        log.setCallerLine(CALLER_LINE);
        log.setCallerMethod(METHOD_NAME_CALLER);
        log.setLevelString(WARNING);
        log.setReferenceFlag(REFERENCE_FLAG);
        log.setThreadName(THREAD_NAME);
        log.setLoggerName(LOGGER_NAME);
        log.setCallerFilename(CALLER_FILENAME);

        assertThat(log.getTimestmp()).isEqualTo(TIMESTMP);
        assertThat(log.getEventId()).isEqualTo(EVENT_ID);
        assertThat(log.getArg0()).isEqualTo(ARG_0);
        assertThat(log.getArg1()).isEqualTo(ARG_1);
        assertThat(log.getArg2()).isEqualTo(ARG_2);
        assertThat(log.getArg3()).isEqualTo(ARG_3);
        assertThat(log.getCallerClass()).isEqualTo(CALLER_CLASS_NAME);
        assertThat(log.getFormattedMessage()).isEqualTo(LONG_FORMATTED_MESSAGE);
        assertThat(log.getCallerLine()).isEqualTo(CALLER_LINE);
        assertThat(log.getCallerMethod()).isEqualTo(METHOD_NAME_CALLER);
        assertThat(log.getLevelString()).isEqualTo(WARNING);
        assertThat(log.getReferenceFlag()).isEqualTo(REFERENCE_FLAG);
        assertThat(log.getThreadName()).isEqualTo(THREAD_NAME);
        assertThat(log.getLoggerName()).isEqualTo(LOGGER_NAME);
        assertThat(log.getCallerFilename()).isEqualTo(CALLER_FILENAME);
    }

    @Test
    void testProperties() {
        LoggingEvent log = new LoggingEvent();
        log.setTimestmp(TIMESTMP);
        log.setEventId(EVENT_ID);

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
        Assertions.assertThat(log.getProperties()).hasSize(2);
    }
}

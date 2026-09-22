/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class LoggingEventExceptionTest {

    private static final String TRACE_LINE_STRING = "Trace Line String";
    private static final int I = 127;

    @Test
    void testGetteresAndSetters() {
        LoggingEvent log = new LoggingEvent();
        log.setTimestmp(1222333444L);
        log.setEventId(125L);

        LoggingEventException logEE = new LoggingEventException();
        logEE.setI(I);
        logEE.setTraceLine(TRACE_LINE_STRING);
        logEE.setLoggingEvent(log);

        Assertions.assertThat(logEE.getTraceLine()).isEqualTo(TRACE_LINE_STRING);
        Assertions.assertThat(logEE.getI()).isEqualTo(I);
        Assertions.assertThat(logEE.getLoggingEvent()).isEqualTo(log);

    }
}

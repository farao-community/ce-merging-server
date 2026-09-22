/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.entity;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class LoggingEventPropertyTest {

    private static final String TSO = "tso";
    private static final String RTE = "RTE";

    @Test
    void testGettersAndSetters() {
        LoggingEventProperty property = new LoggingEventProperty();
        property.setMappedKey(TSO);
        property.setMappedValue(RTE);
        LoggingEvent log = new LoggingEvent();
        log.setTimestmp(1222333444L);
        log.setEventId(125L);
        property.setLoggingEvent(log);

        Assertions.assertThat(property.getLoggingEvent()).isEqualTo(log);
        Assertions.assertThat(property.getMappedKey()).isEqualTo(TSO);
        Assertions.assertThat(property.getMappedValue()).isEqualTo(RTE);

    }
}

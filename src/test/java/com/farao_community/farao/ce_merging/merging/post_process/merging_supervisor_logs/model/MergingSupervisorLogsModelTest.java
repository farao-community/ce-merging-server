/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MergingSupervisorLogsModelTest {

    private static final String TIMESTAMP_STRING = "TIMESTAMP STRING";
    private static final String LEVEL_STRING = "LEVEL STRING";
    private static final String MESSAGE_STRING = "Message String";
    private static final String CONTEXT_STRING = "Contect String";
    private static final String SUB_LEVEL_CONTEXT_STRING = "SUB level context string";

    @Test
    void testModel() {
        MergingSupervisorLogsModel model = new MergingSupervisorLogsModel(TIMESTAMP_STRING, LEVEL_STRING, MESSAGE_STRING, CONTEXT_STRING);
        model.setSubContextLevel1(SUB_LEVEL_CONTEXT_STRING);

        Assertions.assertThat(model.getTimestamp()).isEqualTo(TIMESTAMP_STRING);
        Assertions.assertThat(model.getLevel()).isEqualTo(LEVEL_STRING);
        Assertions.assertThat(model.getMessage()).isEqualTo(MESSAGE_STRING);
        Assertions.assertThat(model.getContext()).isEqualTo(CONTEXT_STRING);
        Assertions.assertThat(model.getSubContextLevel1()).isEqualTo(SUB_LEVEL_CONTEXT_STRING);
    }
}

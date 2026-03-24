/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static test_utils.CeTestUtils.taskWithIdAndStatus;

class MergingServiceTest {

    MergingService service = new MergingService();

    @Test
    void shouldRunTask() {
        assertDoesNotThrow(() -> service.run(taskWithIdAndStatus(1, CREATED)));
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static test_utils.CeTestUtils.ID_1;

class JsonApiDataImplementorsTest {
    static Map<String, JsonApiData> withId1ByType = new HashMap<>();

    static {
        final MergingTaskDto mergingTaskDto = new MergingTaskDto();
        mergingTaskDto.setTaskId(ID_1);
        mergingTaskDto.setTaskName("Test");
        withId1ByType.put("merging-task", mergingTaskDto);
    }

    @Test
    void implementorsShouldHaveTwoGetters() {
        withId1ByType.forEach((expectedType, instance) -> {
            assertThat(instance.getId()).isEqualTo(ID_1);
            assertThat(instance.getType()).isEqualTo(expectedType);
        });

    }

}

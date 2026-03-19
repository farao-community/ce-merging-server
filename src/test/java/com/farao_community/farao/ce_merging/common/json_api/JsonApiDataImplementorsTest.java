/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonApiDataImplementorsTest {
    static List<JsonApiData> withId1AndTypeTest = new ArrayList<>();

    static {
        final MergingTaskDto mergingTaskDto = new MergingTaskDto();
        mergingTaskDto.setTaskId(1);
        mergingTaskDto.setName("Test");
        withId1AndTypeTest.add(mergingTaskDto);
    }

    @ParameterizedTest
    @FieldSource("withId1AndTypeTest")
    void implementorsShouldHaveTwoGetters(final JsonApiData data) {
        assertThat(data.getId())
            .isEqualTo(1);
        assertThat(data.getType())
            .isEqualTo("Test");
    }

}

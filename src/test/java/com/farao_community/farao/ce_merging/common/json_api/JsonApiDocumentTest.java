/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.json_api.JsonApiTestResources.JSON_DOC_IO_EXCEPTION;
import static com.farao_community.farao.ce_merging.common.json_api.JsonApiTestResources.JSON_DOC_ONE_TASK;
import static com.farao_community.farao.ce_merging.common.json_api.JsonApiTestResources.JSON_DOC_TWO_TASKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.CeTestUtils.stringify;

class JsonApiDocumentTest {
    @Test
    void shouldBuildJsonDocumentFromException() throws IOException {
        final String jsonError = stringify(JsonApiDocument.fromServiceException(S_IO_EXCEPTION));
        assertEquals(JSON_DOC_IO_EXCEPTION, jsonError);
    }

    @Test
    void shouldBuildJsonDocumentFromMergingTaskDto() throws IOException {
        assertEquals(JSON_DOC_ONE_TASK, stringify(JsonApiDocument.fromData(new MergingTaskDto())));
        assertEquals(JSON_DOC_TWO_TASKS, stringify(
                         JsonApiDocument.fromDataList(
                             List.of(new MergingTaskDto(), new MergingTaskDto()))
                     )
        );
    }
}

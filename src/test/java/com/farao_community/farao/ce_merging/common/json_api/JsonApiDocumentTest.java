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

import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.CeTestUtils.stringify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonApiDocumentTest {
    @Test
    void shouldBuildJsonDocumentFromException() throws IOException {
        final String jsonError = stringify(JsonApiDocument.fromServiceException(S_IO_EXCEPTION));
        assertEquals("{\"errors\":[{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\"," +
                     "\"title\":\"IO exception\",\"detail\":\"Test\"}]}", jsonError);
    }

    @Test
    void shouldBuildJsonDocumentFromMergingTaskDto() throws IOException {
        assertEquals("{\"data\":[{\"taskId\":0,\"name\":null,\"taskStatus\":null,\"inputs\":null," +
                     "\"configurations\":null,\"outputs\":null,\"artifacts\":null,\"id\":0,\"type\":\"merging-task\"}]}",
                     stringify(JsonApiDocument.fromData(new MergingTaskDto())));
        assertEquals("{\"data\":[{\"taskId\":0,\"name\":null,\"taskStatus\":null,\"inputs\":null," +
                     "\"configurations\":null,\"outputs\":null,\"artifacts\":null,\"id\":0,\"type\":\"merging-task\"}," +
                     "{\"taskId\":0,\"name\":null,\"taskStatus\":null,\"inputs\":null," +
                     "\"configurations\":null,\"outputs\":null,\"artifacts\":null,\"id\":0,\"type\":\"merging-task\"}]}",
                     stringify(JsonApiDocument.fromDataList(List.of(new MergingTaskDto(),
                                                                    new MergingTaskDto()))));
    }
}

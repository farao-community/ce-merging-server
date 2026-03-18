/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

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
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.io.IOException;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.json_api.JsonApiTestResources.JSON_DOC_TWO_ERRORS;
import static com.farao_community.farao.ce_merging.common.json_api.JsonApiTestResources.JSON_DOC_TEST_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.CeTestUtils.stringify;

class JsonApiErrorDocumentTest {
    @Test
    void shouldBuildJsonErrorDocumentFromException() throws IOException {
        final Exception error = new Exception("Test");
        final String jsonError = stringify(JsonApiErrorDocument.fromError(error, "500", "TEST"));
        assertEquals(JSON_DOC_TEST_EXCEPTION, jsonError);
    }

    @Test
    void shouldBuildJsonErrorDocumentFromJsonErrors() throws IOException {
        final JsonApiError serviceError = JsonApiError.fromServiceException(S_IO_EXCEPTION);
        final String jsonErrors = stringify(JsonApiErrorDocument.fromErrors(List.of(serviceError, serviceError)));
        assertEquals(JSON_DOC_TWO_ERRORS, jsonErrors);
    }

    @Test
    void shouldBuildJsonErrorDocumentFromNothing() throws IOException {
        assertEquals("{\"errors\":[]}", stringify(new JsonApiErrorDocument()));
    }

    @Test
    void shouldHaveAccessors() {
        GetterSetterVerifier.forClass(JsonApiErrorDocument.class).verify();
    }

}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.CeTestUtils.stringify;

class JsonApiErrorDocumentTest {
    @Test
    void shouldBuildJsonErrorDocumentFromException() throws IOException {
        final Exception error = new Exception("Test");
        final String jsonError = stringify(JsonApiErrorDocument.fromError(error, "500", "TEST"));
        assertEquals("{\"errors\":[{\"status\":\"500\",\"code\":\"Test\"," +
                     "\"title\":\"TEST\",\"detail\":\"Test\"}]}", jsonError);
    }

    @Test
    void shouldBuildJsonErrorDocumentFromJsonErrors() throws IOException {
        final JsonApiError serviceError = JsonApiError.fromServiceException(S_IO_EXCEPTION);
        final String jsonErrors = stringify(JsonApiErrorDocument.fromErrors(List.of(serviceError, serviceError)));
        assertEquals("{\"errors\":[" +
                     "{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\",\"title\":\"IO exception\",\"detail\":\"Test\"}," +
                     "{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\",\"title\":\"IO exception\",\"detail\":\"Test\"}" +
                     "]}",
                     jsonErrors);
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

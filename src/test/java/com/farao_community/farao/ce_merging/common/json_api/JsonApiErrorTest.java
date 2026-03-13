/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.testServiceEx;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonApiErrorTest {

    @Test
    void shouldBuildJsonErrorFromException() throws IOException {
        final String jsonError = stringify(JsonApiError.fromServiceException(testServiceEx));
        assertEquals("{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\"," +
                     "\"title\":\"IO exception\",\"detail\":\"Test\"}",
                     jsonError);
    }
}

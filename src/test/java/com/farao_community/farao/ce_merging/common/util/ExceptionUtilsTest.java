/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.junit.jupiter.api.Test;
import test_utils.assertions.CeThrowableAssert;

import java.io.IOException;

import static com.farao_community.farao.ce_merging.common.util.ExceptionUtils.logAndThrow;
import static test_utils.CeTestUtils.calls;

class ExceptionUtilsTest {

    private static final String TEST = "test";

    @Test
    void shouldThrowServiceIOException() {
        final IOException cause = new IOException(TEST);

        calls(() -> logAndThrow(null, TEST),
              () -> logAndThrow(cause, TEST),
              () -> logAndThrow(cause, "test %s", "hello"),
              () -> logAndThrow(null, "test %s %s", "hello", "goodbye"))
            .map(CeThrowableAssert::assertThatThrownBy)
            .map(CeThrowableAssert::isServiceException)
            .forEach(t -> t.hasMessageContaining(TEST));
    }

}

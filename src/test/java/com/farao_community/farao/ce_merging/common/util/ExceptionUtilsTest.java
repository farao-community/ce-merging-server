/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.ExceptionUtils.logAndThrow;
import static test_utils.CeTestUtils.throwers;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class ExceptionUtilsTest {

    private static final String TEST = "test";

    static Stream<ThrowableAssert.ThrowingCallable> throwersRunnables() {
        return throwers(() -> logAndThrow(null, TEST),
                        () -> logAndThrow(new IOException(TEST), TEST),
                        () -> logAndThrow(new IOException(TEST), "test %s", "hello"),
                        () -> logAndThrow(null, "test %s %s", "hello", "goodbye"));
    }

    @ParameterizedTest
    @MethodSource("throwersRunnables")
    void shouldThrowServiceIOException(final ThrowableAssert.ThrowingCallable thrower) {
        assertThatThrownBy(thrower)
            .isServiceException()
            .hasMessageContaining(TEST);
    }

}

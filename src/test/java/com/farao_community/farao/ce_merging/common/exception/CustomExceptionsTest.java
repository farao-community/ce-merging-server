/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test_utils.assertions.CeThrowableAssert.assertThat;

class CustomExceptionsTest {

    static List<Class<? extends AbstractServiceException>> exceptionClasses = List.of(CeMergingException.class,
                                                                                      TaskNotValidException.class,
                                                                                      TaskNotFoundException.class,
                                                                                      TaskNotRunException.class,
                                                                                      ServiceIOException.class,
                                                                                      TaskAlreadyRunningException.class);

    @ParameterizedTest
    @FieldSource("exceptionClasses")
    void shouldInstantiateExceptionsFromMessage(final Class<? extends AbstractServiceException> exceptionClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final AbstractServiceException e = exceptionClass
            .getConstructor(String.class)
                .newInstance("testing exceptions");

        assertNotNull(e.getTitle());
        assertNotNull(e.getCode());
        assertNotNull(e.getStatus());
        assertEquals("testing exceptions", e.getMessage());
        assertThat(e).isServiceException();
    }

    @ParameterizedTest
    @FieldSource("exceptionClasses")
    void shouldInstantiateExceptionsFromMessageAndCause(final Class<? extends AbstractServiceException> exceptionClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final IOException cause = new IOException("testing cause");

        final AbstractServiceException e = exceptionClass
            .getConstructor(String.class, Throwable.class)
            .newInstance("testing exceptions", cause);

        assertNotNull(e.getTitle());
        assertNotNull(e.getCode());
        assertNotNull(e.getStatus());
        assertEquals("testing exceptions", e.getMessage());
        assertThat(e).isServiceException();
        assertEquals("testing cause", e.getCause().getMessage());
    }

}

package com.farao_community.farao.ce_merging.common.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CustomExceptionsTest {

    static List<Class<? extends AbstractServiceException>> exceptionClasses = List.of(CeMergingException.class,
                                                                                      InvalidTaskException.class,
                                                                                      ResourceNotFoundException.class,
                                                                                      ResourceNotRunException.class,
                                                                                      ServiceIOException.class,
                                                                                      TaskAlreadyRunningException.class);

    @ParameterizedTest
    @FieldSource("exceptionClasses")
    void shouldInstantiateExceptionsFromMessage(Class<? extends AbstractServiceException> exceptionClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final AbstractServiceException e = exceptionClass
            .getConstructor(String.class)
                .newInstance("testing exceptions");

        assertNotNull(e.getTitle());
        assertNotNull(e.getCode());
        assertNotNull(e.getStatus());
        assertEquals("testing exceptions", e.getMessage());
    }

}
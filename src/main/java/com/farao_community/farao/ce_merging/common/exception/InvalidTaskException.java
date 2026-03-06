/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.common.exception;

public class InvalidTaskException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "400";
    private static final String EXCEPTION_ERROR_CODE = "400-INVALID-TASK";
    private static final String EXCEPTION_TITLE = "Invalid task";

    public InvalidTaskException(String message) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public InvalidTaskException(String message, Throwable throwable) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

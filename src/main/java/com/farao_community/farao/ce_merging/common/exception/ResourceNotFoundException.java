package com.farao_community.farao.ce_merging.common.exception;

public class ResourceNotFoundException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "404";
    private static final String EXCEPTION_ERROR_CODE = "404-RESOURCE-NOT-FOUND";
    private static final String EXCEPTION_TITLE = "Resource not found";

    public ResourceNotFoundException(String message) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public ResourceNotFoundException(String message, Throwable throwable) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

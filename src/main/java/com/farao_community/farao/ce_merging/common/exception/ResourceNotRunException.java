package com.farao_community.farao.ce_merging.common.exception;

public class ResourceNotRunException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "500";
    private static final String EXCEPTION_ERROR_CODE = "500-RESOURCE-NOT-RUN";
    private static final String EXCEPTION_TITLE = "Resource not run";

    public ResourceNotRunException(String message) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public ResourceNotRunException(String message, Throwable throwable) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

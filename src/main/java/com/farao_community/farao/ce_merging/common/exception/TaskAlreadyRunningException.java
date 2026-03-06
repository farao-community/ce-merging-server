package com.farao_community.farao.ce_merging.common.exception;

public class TaskAlreadyRunningException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "400";
    private static final String EXCEPTION_ERROR_CODE = "400-TASK-ALREADY-RUNNING";
    private static final String EXCEPTION_TITLE = "Task already running";

    public TaskAlreadyRunningException(String message) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public TaskAlreadyRunningException(String message, Throwable throwable) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

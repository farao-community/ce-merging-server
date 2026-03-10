/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.BAD_REQUEST;

public class TaskAlreadyRunningException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "400";
    private static final String EXCEPTION_ERROR_CODE = "400-TASK-ALREADY-RUNNING";
    private static final String EXCEPTION_TITLE = "Task already running";

    public TaskAlreadyRunningException(String message) {
        super(BAD_REQUEST, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public TaskAlreadyRunningException(String message, Throwable throwable) {
        super(BAD_REQUEST, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

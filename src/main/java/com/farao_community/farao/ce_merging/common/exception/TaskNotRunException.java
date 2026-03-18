/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.INTERNAL_ERROR;

public class TaskNotRunException extends AbstractServiceException {
    private static final String EXCEPTION_ERROR_CODE = "500-RESOURCE-NOT-RUN";
    private static final String EXCEPTION_TITLE = "Resource not run";

    public TaskNotRunException(final String message) {
        super(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public TaskNotRunException(final String message, final Throwable cause) {
        super(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, cause);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.BAD_REQUEST;

public class InvalidTaskException extends AbstractServiceException {
    private static final String EXCEPTION_ERROR_CODE = "400-INVALID-TASK";
    private static final String EXCEPTION_TITLE = "Invalid task";

    public InvalidTaskException(final String message) {
        super(BAD_REQUEST, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public InvalidTaskException(final String message, final Throwable cause) {
        super(BAD_REQUEST, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, cause);
    }
}

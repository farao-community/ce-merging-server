/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.INTERNAL_ERROR;

public class ServiceIOException extends AbstractServiceException {
    private static final String EXCEPTION_ERROR_CODE = "500-IO-EXCEPTION";
    private static final String EXCEPTION_TITLE = "IO exception";
    private static final String ERROR_OCCURRED_WHILE = "Error occurred while ";

    public ServiceIOException(final String message) {
        super(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public ServiceIOException(final String message,
                              final Throwable cause) {
        super(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, cause);
    }

    public static ServiceIOException errorWhile(final Throwable cause,
                                                final String circumstance,
                                                final Object... args) {
        final String errorMessage = ERROR_OCCURRED_WHILE + circumstance.formatted(args);
        return new ServiceIOException(errorMessage, cause);
    }
}

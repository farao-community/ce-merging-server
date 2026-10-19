/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.INTERNAL_ERROR;

public class CeMergingException extends AbstractServiceException {
    private static final String EXCEPTION_ERROR_CODE = "500-CE-MERGING-RUNTIME-EXCEPTION";
    private static final String EXCEPTION_TITLE = "Runtime exception encountered in ce-merging server";

    public CeMergingException(final String message) {
        this(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public CeMergingException(final String status,
                              final String code,
                              final String title,
                              final String message) {
        super(status, code, title, message);
    }

    public CeMergingException(final String message,
                              final Throwable cause) {
        super(INTERNAL_ERROR, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, cause);
    }

    public static Supplier<CeMergingException> withMessage(final String message) {
        return () -> new CeMergingException(message);
    }

    public static Supplier<CeMergingException> withMessage(final String message, final Object... args) {
        return withMessage(message.formatted(args));
    }
}

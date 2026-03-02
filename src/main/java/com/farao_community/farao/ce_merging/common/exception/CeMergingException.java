/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

public class CeMergingException extends AbstractServiceException {
    private static final String EXCEPTION_HTTP_STATUS = "500";
    private static final String EXCEPTION_ERROR_CODE = "500-CORE-MERGING-RUNTIME-EXCEPTION";
    private static final String EXCEPTION_TITLE = "Runtime exception encountered in core-merging server";

    public CeMergingException(String message) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public CeMergingException(String status, String code, String title, String message) {
        super(status, code, title, message);
    }

    public CeMergingException(String message, Throwable throwable) {
        super(EXCEPTION_HTTP_STATUS, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

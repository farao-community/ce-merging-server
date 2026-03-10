/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NOT_FOUND;

public class ResourceNotFoundException extends AbstractServiceException {
    private static final String EXCEPTION_ERROR_CODE = "404-RESOURCE-NOT-FOUND";
    private static final String EXCEPTION_TITLE = "Resource not found";

    public ResourceNotFoundException(String message) {
        super(NOT_FOUND, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message);
    }

    public ResourceNotFoundException(String message, Throwable throwable) {
        super(NOT_FOUND, EXCEPTION_ERROR_CODE, EXCEPTION_TITLE, message, throwable);
    }
}

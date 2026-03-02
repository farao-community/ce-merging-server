/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.exception;

public abstract class AbstractServiceException extends RuntimeException {
    private final String status;
    private final String code;
    private final String title;

    protected AbstractServiceException(final String status,
                                       final String code,
                                       final String title,
                                       final String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    protected AbstractServiceException(final String status,
                                       final String code,
                                       final String title,
                                       final String message,
                                       final Throwable throwable) {
        super(message, throwable);
        this.status = status;
        this.code = code;
        this.title = title;
    }

}

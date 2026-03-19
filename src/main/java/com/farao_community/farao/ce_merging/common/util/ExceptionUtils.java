/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExceptionUtils {

    private ExceptionUtils() {
        // utility class
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionUtils.class);
    private static final String ERROR_OCCURRED_WHILE = "Error occurred while ";

    /**
     *
     * @param cause        caught exception
     * @param circumstance to be a tad more concise since all messages starts with the same words
     * @param args         if the message is to be formatted
     * @param <T>          to have something to return everywhere
     * @return just to compile
     */
    public static <T> T logAndThrow(final Throwable cause,
                                    final String circumstance,
                                    final Object... args) {
        final String errorMessage = ERROR_OCCURRED_WHILE + circumstance.formatted(args);
        LOGGER.error(errorMessage, cause);
        throw new ServiceIOException(errorMessage, cause);
    }
}

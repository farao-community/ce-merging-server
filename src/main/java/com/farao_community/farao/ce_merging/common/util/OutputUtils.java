/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public final class OutputUtils {

    private OutputUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(OutputUtils.class);

    public static int calculateTargetPosition(OffsetDateTime targetDate, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        if (!isValidInterval(targetDate, periodStart, periodEnd)) {
            LOGGER.error("Process target date {} is out of daily time interval [{}, {}]", targetDate, periodStart, periodEnd);
            throw new CeMergingException("Process target date {} is out of daily time interval [{}, {}]", targetDate.toString(), periodStart.toString(), periodEnd.toString());
        } else {
            int position = 1;
            OffsetDateTime intervalStart = periodStart;
            OffsetDateTime intervalEnd = periodStart.plusHours(1);
            while (!isValidInterval(targetDate, intervalStart, intervalEnd)) {
                position = position + 1;
                intervalStart = intervalEnd;
                intervalEnd = intervalStart.plusHours(1);
            }
            return position;
        }
    }

    private static boolean isValidInterval(OffsetDateTime targetDate, OffsetDateTime intervalStart, OffsetDateTime intervalEnd) {
        return !targetDate.isBefore(intervalStart) && targetDate.isBefore(intervalEnd);
    }

    public static String getDocumentIdentificationDate(String dailyTimeInterval) {
        return dailyTimeInterval.substring(18, 28).replace("-", "");
    }

}

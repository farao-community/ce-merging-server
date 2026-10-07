/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.DocumentTypeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class OutputUtils {

    public static final Logger LOGGER = LoggerFactory.getLogger(OutputUtils.class);
    private static final String OUTPUT_NAME = "22XCORESO------S_10V1001C--00236Y_CORE-FB-%s%s-%03d_%s-F%03d-%02d.%s";
    private static final String OUTPUT_NAME_WITHOUT_MESSAGE_DOCUMENT_TYPE = "22XCORESO------S_10V1001C--00236Y_CORE-FB-%03d_%s-F%03d-%02d.%s";
    public static final String DAYLIGHT_DUPLICATED_HOUR = "02";
    public static final String DAYLIGHT_DUPLICATED_HOUR_NAME_CONVENTION = "B";
    public static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    public static String generateOutputFileName(final OffsetDateTime mergingDateTime,
                                                final int mergingVersion,
                                                final String messageType,
                                                final DocumentTypeList documentType,
                                                final int flow,
                                                final String extension) {
        final String mergingDate = DateTimeUtils.formatDate(mergingDateTime);
        return String.format(OUTPUT_NAME, messageType, documentType.value(), flow, mergingDate, flow, mergingVersion, extension);
    }

    public static String generateOutputFileName(OffsetDateTime mergingDateTime, int mergingVersion, int flow, String extension) {
        final String mergingDate = DateTimeUtils.formatDate(mergingDateTime);
        return String.format(OUTPUT_NAME_WITHOUT_MESSAGE_DOCUMENT_TYPE, flow, mergingDate, flow, mergingVersion, extension);
    }

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

    public static String getDocumentIdentificationDate(String dailyTimeInterval) {
        return dailyTimeInterval.substring(18, 28).replace("-", "");
    }

    private OutputUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    private static boolean isValidInterval(OffsetDateTime targetDate, OffsetDateTime intervalStart, OffsetDateTime intervalEnd) {
        return !targetDate.isBefore(intervalStart) && targetDate.isBefore(intervalEnd);
    }
}

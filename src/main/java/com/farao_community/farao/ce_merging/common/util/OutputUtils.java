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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class OutputUtils {

    private OutputUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static final Logger LOGGER = LoggerFactory.getLogger(OutputUtils.class);

    private static final String OUTPUT_NAME = "22XCORESO------S_10V1001C--00236Y_CORE-FB-%s%s-%03d_%s-F%03d-%02d.%s";
    private static final String OUTPUT_NAME_WITHOUT_MESSAGE_DOCUMENT_TYPE = "22XCORESO------S_10V1001C--00236Y_CORE-FB-%03d_%s-F%03d-%02d.%s";

    public static String generateOutputFileName(OffsetDateTime mergingDateTime, int mergingVersion, String messageType, String documentType, int flow, String extension) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String mergingDate = fmt.format(mergingDateTime);
        return String.format(OUTPUT_NAME, messageType, documentType, flow, mergingDate, flow, mergingVersion, extension);
    }

    public static String generateOutputFileName(OffsetDateTime mergingDateTime, int mergingVersion, int flow, String extension) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        String mergingDate = fmt.format(mergingDateTime);
        return String.format(OUTPUT_NAME_WITHOUT_MESSAGE_DOCUMENT_TYPE, flow, mergingDate, flow, mergingVersion, extension);
    }

    public static XMLGregorianCalendar getXMLGregorianCurrentTime() {
        XMLGregorianCalendar xmlGregorianCalendar;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(OffsetDateTime.now().toString());
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlGregorianCalendar.setTimezone(0);
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("Impossible to create XmlGregorianCalendar current time");
            throw new CeMergingException("Impossible to create XmlGregorianCalendar current time", e);
        }
        return xmlGregorianCalendar;
    }

    public static OffsetDateTime convertToZFormat(OffsetDateTime targetDate) {
        Instant date = Instant.from(targetDate);
        return OffsetDateTime.parse(date.toString(), DateTimeFormatter.ISO_DATE_TIME);
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

    private static boolean isValidInterval(OffsetDateTime targetDate, OffsetDateTime intervalStart, OffsetDateTime intervalEnd) {
        return !targetDate.isBefore(intervalStart) && targetDate.isBefore(intervalEnd);
    }

    public static String getDocumentIdentificationDate(String dailyTimeInterval) {
        return dailyTimeInterval.substring(18, 28).replace("-", "");
    }

}

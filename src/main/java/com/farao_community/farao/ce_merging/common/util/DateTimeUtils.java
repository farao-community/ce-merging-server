/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DATE_TIME_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;

public final class DateTimeUtils {
    private static final DateTimeFormatter TARGET_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).withLocale(Locale.FRANCE);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("e").withLocale(Locale.FRANCE);
    private static final int UTC_TIMEZONE_OFFSET = 0;
    private static final DateTimeFormatter SIMPLE_DATETIME_NO_TIMEZONE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateTimeUtils() {
    }

    public static String toStringNoTimeZone(final Temporal dateTime) {
        return SIMPLE_DATETIME_NO_TIMEZONE_FORMATTER.format(dateTime);
    }

    public static String formatTargetDate(final MergingTask task) {
        return TARGET_DATE_FORMATTER.format(getTargetDateAtParisZone(task));
    }

    public static String dayOfWeek(final MergingTask task) {
        return DAY_OF_WEEK_FORMATTER.format(getTargetDateAtParisZone(task));
    }

    private static ZonedDateTime getTargetDateAtParisZone(final MergingTask task) {
        return task.getInputs().getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
    }

    static ZonedDateTime nowAtParisZone() {
        return ZonedDateTime.now(PARIS_ZONE_ID);
    }

    public static String toHourlyInterval(final OffsetDateTime targetDateTime) {
        final Instant startInstant = targetDateTime.withMinute(0).toInstant();
        final Instant endInstant = startInstant.plus(Duration.ofHours(1));
        return String.format("%s/%s", OffsetDateTime.parse(startInstant.toString()), OffsetDateTime.parse(endInstant.toString()));
    }

    public static XMLGregorianCalendar getNowDate() {
        try {
            final GregorianCalendar calendar = GregorianCalendar.from(DateTimeUtils.nowAtParisZone());
            final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlGregorianCalendar.setTimezone(UTC_TIMEZONE_OFFSET);
            return xmlGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            throw new CeMergingException("Cannot create XMLGregorianCalendar date for fixed glsk document, " + e.getMessage());
        }
    }
}

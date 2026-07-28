/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DATE_TIME_FORMAT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;

public final class DateTimeUtils {
    private static final DateTimeFormatter TARGET_DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).withLocale(Locale.FRANCE);
    private static final DateTimeFormatter DAY_OF_WEEK_FORMATTER = DateTimeFormatter.ofPattern("e").withLocale(Locale.FRANCE);

    private DateTimeUtils() {
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
}

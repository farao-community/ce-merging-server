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

    private DateTimeUtils() {
    }

    public static String formatTargetDate(final MergingTask task) {
        final ZonedDateTime targetDate = task.getInputs().getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
        return DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)
                .withLocale(Locale.FRANCE)
                .format(targetDate);
    }

    public static String dayOfWeek(final MergingTask task) {
        final ZonedDateTime targetDate = task.getInputs().getTargetDate().atZoneSameInstant(PARIS_ZONE_ID);
        return DateTimeFormatter.ofPattern("e")
                .withLocale(Locale.FRANCE)
                .format(targetDate);
    }
}

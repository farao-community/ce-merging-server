/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.merging_request;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toZFormat;

public record RequestInformation(
        String requestTimeInterval,
        String noun,
        String context,
        String replyAddress,
        String correlationID
) {

    public OffsetDateTime getMergingDay() {
        return OffsetDateTime.parse(
                requestTimeInterval.substring(18, 35),
                DateTimeFormatter.ISO_DATE_TIME
        );
    }

    public OffsetDateTime getStartDateTime() {
        return OffsetDateTime.parse(
                requestTimeInterval.substring(0, 17),
                DateTimeFormatter.ISO_DATE_TIME
        );
    }

    public OffsetDateTime getEndDateTime() {
        return OffsetDateTime.parse(
                requestTimeInterval.substring(18, 35),
                DateTimeFormatter.ISO_DATE_TIME
        );
    }

    public List<String> findAllIntervals() {
        final List<String> intervalList = new ArrayList<>();
        final OffsetDateTime start = getStartDateTime();
        final OffsetDateTime end = getEndDateTime();

        for (long i = 0; !start.plusHours(i).equals(end); i++) {
            intervalList.add(toZFormat(start.plusHours(i)) + "/" + toZFormat(start.plusHours(i + 1)));
        }
        return intervalList;
    }

}

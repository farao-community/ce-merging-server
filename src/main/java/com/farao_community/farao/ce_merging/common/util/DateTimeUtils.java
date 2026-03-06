/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@NoArgsConstructor(access = AccessLevel.NONE)
public class DateTimeUtils {

    public static final ZoneId ZONE_OF_PARIS = ZoneId.of("Europe/Paris");

    public static OffsetDateTime convertToZFormat(final OffsetDateTime offsetDateTime) {
        return OffsetDateTime.parse(Instant.from(offsetDateTime).toString(), ISO_DATE_TIME);
    }
}

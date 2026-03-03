package com.farao_community.farao.ce_merging.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.OffsetDateTime;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@NoArgsConstructor(access = AccessLevel.NONE)
public class DateTimeUtils {

    public static OffsetDateTime convertToZFormat(final OffsetDateTime offsetDateTime) {
        return OffsetDateTime.parse(Instant.from(offsetDateTime).toString(), ISO_DATE_TIME);
    }
}

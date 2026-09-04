/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.enums;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILENAME_DATETIME_FMT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;
import static java.util.Locale.FRANCE;

public enum OutputType {

    MERGING_LOGS("%s_CORESO_MergingLogs.xml", "merging-logs"),
    REF_PROG("%s_CORESO_RefProg.xml", "ref-prog");

    private final String fileName;
    private final String location;

    OutputType(final String fileName, final String location) {
        this.fileName = fileName;
        this.location = location;
    }

    public String getFileName(final OffsetDateTime targetDate) {
        final ZonedDateTime targetZdtParis = targetDate.atZoneSameInstant(PARIS_ZONE_ID);
        final String dateAndTime = FILENAME_DATETIME_FMT.withLocale(FRANCE).format(targetZdtParis);
        return fileName.formatted(dateAndTime);
    }

    public String getLocation(final long taskId) {
        return String.format("/tasks/%d/outputs/%s", taskId, location);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DateTimeUtilsTest {
    @Test
    void shouldFormatTargetDate() {
        final MergingTask task = mock(MergingTask.class);
        when(task.getTargetDateInParis()).thenReturn(ZonedDateTime.parse("2024-05-14T12:00:00Z"));
        assertEquals("20240514_1200", DateTimeUtils.formatTargetDate(task));
    }

    @Test
    void shouldReturnDayOfWeek() {
        final MergingTask task = mock(MergingTask.class);
        when(task.getTargetDateInParis()).thenReturn(ZonedDateTime.parse("2026-07-08T12:00:00Z"));
        assertEquals("3", DateTimeUtils.dayOfWeek(task));
    }
}

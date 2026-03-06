/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus.SUCCESS;
import static java.util.function.Predicate.not;

@NoArgsConstructor(access = AccessLevel.NONE)
public class MergingTaskPredicates {

    public static Predicate<MergingTask> isSuccessful() {
        return task -> SUCCESS.equals(task.getTaskStatus());
    }

    public static Predicate<MergingTask> isBefore(final OffsetDateTime dateTime) {
        return task -> task.getInputs().getTargetDate().isBefore(dateTime);
    }

    public static Predicate<MergingTask> isBetween(final OffsetDateTime startInclusive,
                                                   final OffsetDateTime endExclusive) {
        return isBefore(endExclusive).and(not(isBefore(startInclusive)));
    }
}

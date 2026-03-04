package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.entities.CeMergingTaskEntity;

import java.time.OffsetDateTime;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.entities.enums.TaskStatus.SUCCESS;
import static java.util.function.Predicate.not;

public class TaskPredicates {

    public static Predicate<CeMergingTaskEntity> isSuccessful() {
        return task -> SUCCESS.equals(task.getTaskStatus());
    }

    public static Predicate<CeMergingTaskEntity> isBefore(final OffsetDateTime dateTime) {
        return task -> task.getInputs().getTargetDate().isBefore(dateTime);
    }

    public static Predicate<CeMergingTaskEntity> isBetween(final OffsetDateTime startInclusive,
                                                           final OffsetDateTime endExclusive) {
        return isBefore(endExclusive).and(not(isBefore(startInclusive)));
    }
}

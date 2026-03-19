/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils.assertions;

import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.Objects;

public class CeTaskAssert extends AbstractAssert<CeTaskAssert, MergingTask> {

    protected CeTaskAssert(final MergingTask mergingTask) {
        super(mergingTask, CeTaskAssert.class);
    }

    public static CeTaskAssert assertThat(final MergingTask task) {
        return new CeTaskAssert(task);
    }

    @CanIgnoreReturnValue
    public CeTaskAssert hasStatus(final TaskStatus expected) {
        if (actual.getTaskStatus() != expected) {
            failWithActualExpectedAndMessage(actual.getTaskStatus(), expected, "Unexpected task status");
        }
        return this;
    }

    @CanIgnoreReturnValue
    public CeTaskAssert isSameTaskAs(final MergingTask other) {
        if (actual.getTaskId() != other.getTaskId()
            || actual.getTaskStatus() != other.getTaskStatus()
            || !Objects.equals(actual.getName(), other.getName())
            || !Objects.equals(actual.getArchiveFileOriginalName(), other.getArchiveFileOriginalName())) {
            failWithMessage("These are not the same tasks");
        }
        return this;
    }

    @CanIgnoreReturnValue
    public CeTaskAssert isSameTaskAs(final MergingTaskDto dto) {
        if (actual.getTaskId() != dto.getTaskId()
            || actual.getTaskStatus() != dto.getTaskStatus()
            || !Objects.equals(actual.getName(), dto.getName())) {
            failWithMessage("These are not the same tasks");
        }
        return this;
    }

}

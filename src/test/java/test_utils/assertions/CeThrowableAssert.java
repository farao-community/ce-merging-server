/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils.assertions;

import com.farao_community.farao.ce_merging.common.exception.AbstractServiceException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotRunException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotValidException;
import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ThrowableAssert;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CeThrowableAssert<T extends Throwable> extends AbstractThrowableAssert<CeThrowableAssert<T>, T> {

    @SuppressWarnings("unchecked") // NOSONAR because E extends Throwable
    public static <E extends Throwable> CeThrowableAssert<E> assertThatThrownBy(ThrowableAssert.ThrowingCallable shouldRaiseThrowable) {
        return new CeThrowableAssert<>((E) catchThrowable(shouldRaiseThrowable));
    }

    public static <E extends Throwable> CeThrowableAssert<E> assertThat(E actual) {
        return new CeThrowableAssert<>(actual);
    }

    protected CeThrowableAssert(final T actual) {
        super(actual, CeThrowableAssert.class);
    }

    public CeThrowableAssert<T> isValidServiceException() {

        if (actual instanceof AbstractServiceException abstractServiceException) {
            assertNotNull(abstractServiceException.getTitle());
            assertNotNull(abstractServiceException.getCode());
            assertNotNull(abstractServiceException.getStatus());
        }
        else {
            failWithMessage("%s is not a service exception",
                            actual.getClass().getName());
        }


        return this;

    }

    public CeThrowableAssert<T> isTaskException() {
        return this.isOfAnyClassIn(TaskAlreadyRunningException.class,
                                                          TaskNotFoundException.class,
                                                          TaskNotRunException.class,
                                                          TaskNotValidException.class);
    }
}

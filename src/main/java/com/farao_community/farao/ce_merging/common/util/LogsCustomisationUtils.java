/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.slf4j.MDC;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_STEP;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO_LOWER_CASE;

public final class LogsCustomisationUtils {

    private static final String TASK_ID_EXTRA_FIELD = "taskId";

    private LogsCustomisationUtils() {
    }

    public static void setExtraFieldsInLogsMdc(long taskId, String mergingStepValue) {
        // propagate extra fields into SLF4J's MDC in current service
        MDC.put(TASK_ID_EXTRA_FIELD, Long.toString(taskId));
        MDC.put(MERGING_STEP, mergingStepValue);

    }

    public static void setTsoExtraFieldInLogsMdc(String tso) {
        MDC.put(TSO_LOWER_CASE, tso);
    }

    public static void removeTsoFieldFromMdc() {
        MDC.remove(TSO_LOWER_CASE);
    }
}

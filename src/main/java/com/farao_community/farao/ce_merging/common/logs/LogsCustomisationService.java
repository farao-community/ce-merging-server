/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.logs;

import brave.Tracer;
import brave.baggage.BaggageField;
import brave.propagation.TraceContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class LogsCustomisationService {

    private static final String TASK_ID_EXTRA_FIELD = "taskId";
    private static final String TSO_EXTRA_FIELD = "tso";
    private static final String MERGING_STEP_EXTRA_FIELD = "merging-step";

    private final Tracer tracer;

    public LogsCustomisationService(Tracer tracer) {
        this.tracer = tracer;
    }

    public void setExtraFieldsInLogsMdc(final long taskId, final String mergingStepValue) {
        // propagate extra fields into SLF4J's MDC in current service
        MDC.put(TASK_ID_EXTRA_FIELD, Long.toString(taskId));
        MDC.put(MERGING_STEP_EXTRA_FIELD, mergingStepValue);

        // propagate extra fields into SLF4J's MDC in client services
        BaggageField taskIdField = BaggageField.getByName(currentContext(), TASK_ID_EXTRA_FIELD);
        taskIdField.updateValue(Long.toString(taskId));

        BaggageField mergingStepField = BaggageField.getByName(currentContext(), MERGING_STEP_EXTRA_FIELD);
        mergingStepField.updateValue(mergingStepValue);
    }

    public void setTsoExtraFieldInLogsMdc(final String tso) {
        MDC.put(TSO_EXTRA_FIELD, tso);
        BaggageField tsoField = BaggageField.getByName(currentContext(), TSO_EXTRA_FIELD);
        tsoField.updateValue(tso);
    }

    public void removeTsoFieldFromMdc() {
        MDC.remove(TSO_EXTRA_FIELD);
        BaggageField tsoField = BaggageField.getByName(currentContext(), TSO_EXTRA_FIELD);
        tsoField.updateValue("");
    }

    private TraceContext currentContext() {
        return tracer.currentSpan().context();
    }
}

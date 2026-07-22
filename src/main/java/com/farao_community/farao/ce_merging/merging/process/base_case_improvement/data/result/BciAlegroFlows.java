/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroFlows;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BciAlegroFlows(@JsonProperty("targetFlow") double targetFlow,
                             @JsonProperty("minEc") double minEc,
                             @JsonProperty("maxEc") double maxEc) {

    public BciAlegroFlows(final AlegroFlows alegroFlows,
                          final Interval interval) {
        this(alegroFlows.targetFlow(), interval.getMinValue(), interval.getMaxValue());
    }

}

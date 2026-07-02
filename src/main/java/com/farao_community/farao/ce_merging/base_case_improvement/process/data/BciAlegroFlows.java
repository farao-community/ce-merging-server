/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BciAlegroFlows {
    private double targetFlow;
    private double minEc;
    private double maxEc;

    public BciAlegroFlows(@JsonProperty("targetFlow") final double targetFlow,
                          @JsonProperty("minEc") final double minEc,
                          @JsonProperty("maxEc") final double maxEc) {
        this.targetFlow = targetFlow;
        this.minEc = minEc;
        this.maxEc = maxEc;
    }

    public double getTargetFlow() {
        return targetFlow;
    }

    public void setTargetFlow(final double targetFlow) {
        this.targetFlow = targetFlow;
    }

    public double getMinEc() {
        return minEc;
    }

    public void setMinEc(final double minEc) {
        this.minEc = minEc;
    }

    public double getMaxEc() {
        return maxEc;
    }

    public void setMaxEc(final double maxEc) {
        this.maxEc = maxEc;
    }
}

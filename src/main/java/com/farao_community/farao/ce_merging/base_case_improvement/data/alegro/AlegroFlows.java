/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.alegro;

public class AlegroFlows {
    private double initialFlow;
    private double targetFlow;
    private double gapNpfInitialFlow;

    public AlegroFlows() {
    }

    public double getInitialFlow() {
        return initialFlow;
    }

    public void setInitialFlow(final double initialFlow) {
        this.initialFlow = initialFlow;
    }

    public double getTargetFlow() {
        return targetFlow;
    }

    public void setTargetFlow(final double targetFlow) {
        this.targetFlow = targetFlow;
    }

    public double getGapNpfInitialFlow() {
        return gapNpfInitialFlow;
    }

    public void setGapNpfInitialFlow(final double gapNpfInitialFlow) {
        this.gapNpfInitialFlow = gapNpfInitialFlow;
    }
}

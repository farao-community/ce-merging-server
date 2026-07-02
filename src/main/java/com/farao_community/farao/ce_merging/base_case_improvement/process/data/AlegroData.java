/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process.data;

import java.io.Serializable;

public class AlegroData implements Serializable {

    private boolean isInOutage;
    private AlegroFlows alDeFlows;
    private AlegroFlows alBeFlows;

    public AlegroData() {
    }

    public AlegroFlows getAlDeFlows() {
        return alDeFlows;
    }

    public void setAlDeFlows(final AlegroFlows alDeFlows) {
        this.alDeFlows = alDeFlows;
    }

    public AlegroFlows getAlBeFlows() {
        return alBeFlows;
    }

    public void setAlBeFlows(final AlegroFlows alBeFlows) {
        this.alBeFlows = alBeFlows;
    }

    public boolean isInOutage() {
        return isInOutage;
    }

    public void setInOutage(final boolean isInOutage) {
        this.isInOutage = isInOutage;
    }
}

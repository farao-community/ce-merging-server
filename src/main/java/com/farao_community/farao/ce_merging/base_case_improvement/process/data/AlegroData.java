/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process.data;

import java.io.Serializable;

public class AlegroData implements Serializable {

    private Boolean alegroInOutage;
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

    public Boolean getAlegroInOutage() {
        return alegroInOutage;
    }

    public void setAlegroInOutage(final Boolean alegroInOutage) {
        this.alegroInOutage = alegroInOutage;
    }
}

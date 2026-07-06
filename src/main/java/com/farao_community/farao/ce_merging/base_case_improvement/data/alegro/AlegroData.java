/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.alegro;

import java.io.Serializable;

public class AlegroData implements Serializable {

    private boolean alegroInOutage;
    private AlegroFlows aldeFlows;
    private AlegroFlows albeFlows;

    public AlegroData() {
    }

    public AlegroFlows getAldeFlows() {
        return aldeFlows;
    }

    public void setAldeFlows(final AlegroFlows aldeFlows) {
        this.aldeFlows = aldeFlows;
    }

    public AlegroFlows getAlbeFlows() {
        return albeFlows;
    }

    public void setAlbeFlows(final AlegroFlows albeFlows) {
        this.albeFlows = albeFlows;
    }

    public boolean isAlegroInOutage() {
        return alegroInOutage;
    }

    public void setAlegroInOutage(final boolean isInOutage) {
        this.alegroInOutage = isInOutage;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.alegro;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BciAlegroData {

    private BciAlegroFlows aldeFlows;
    private BciAlegroFlows albeFlows;

    @JsonCreator
    public BciAlegroData(@JsonProperty("aldeFlows") final BciAlegroFlows aldeFlows,
                         @JsonProperty("albeFlows") final BciAlegroFlows albeFlows) {
        this.aldeFlows = aldeFlows;
        this.albeFlows = albeFlows;
    }

    public BciAlegroFlows getAldeFlows() {
        return aldeFlows;
    }

    public void setAldeFlows(final BciAlegroFlows aldeFlows) {
        this.aldeFlows = aldeFlows;
    }

    public BciAlegroFlows getAlbeFlows() {
        return albeFlows;
    }

    public void setAlbeFlows(final BciAlegroFlows albeFlows) {
        this.albeFlows = albeFlows;
    }
}

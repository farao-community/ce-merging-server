/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * WARNING: this class is linked to the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class RecessivityParameters {
    /**
     * Contains the recessive IGMs country codes
     * By default, recessive countries are configured under defaultConfigurations/default-recessivity-parameters.json
     * recessive means that it will not impose the status of the X-Node in case of inconsistency
     */
    private List<String> recessiveCountries;

    @JsonCreator
    public RecessivityParameters(@JsonProperty("recessiveCountries") final List<String> recessiveCountries) {
        this.recessiveCountries = recessiveCountries;
    }

    public List<String> getRecessiveCountries() {
        return recessiveCountries;
    }

    public void setRecessiveCountries(final List<String> recessiveCountries) {
        this.recessiveCountries = recessiveCountries;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record InitialNetPositions(Map<String, CountryNetPositions> countryNetPositionsMap) {

    @JsonCreator
    public InitialNetPositions(@JsonProperty("netPositionsByCountryMap") final Map<String, CountryNetPositions> countryNetPositionsMap) {
        this.countryNetPositionsMap = countryNetPositionsMap;
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.model.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Country;

import java.util.Map;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public record NetPositionsResults(Map<String, NetPositions> netPositionsByCountryMap) {
    @JsonCreator
    public NetPositionsResults(@JsonProperty("netPositionsByCountryMap") final Map<String, NetPositions> netPositionsByCountryMap) {
        this.netPositionsByCountryMap = netPositionsByCountryMap;
    }

    public NetPositions get(final Country country) {
        return netPositionsByCountryMap.get(country.name());
    }
}

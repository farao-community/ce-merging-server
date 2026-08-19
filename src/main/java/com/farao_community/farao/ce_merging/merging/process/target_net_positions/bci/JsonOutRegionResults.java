/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class JsonOutRegionResults {

    @JsonProperty("globalForecastNetPositions")
    private Map<String, Double> globalForecastNetPositions;

    public JsonOutRegionResults() {
    }

    public Map<String, Double> getGlobalForecastNetPositions() {
        return globalForecastNetPositions;
    }
}

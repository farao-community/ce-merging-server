/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record OutRegionResults(Map<String, Double> globalForecastNetPositions) {
    @JsonCreator
    public OutRegionResults(@JsonProperty("GlobalForecastNetPositions") final Map<String, Double> globalForecastNetPositions) {
        this.globalForecastNetPositions = globalForecastNetPositions;
    }
}

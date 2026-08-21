/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonGlobalNetPositions {

    @JsonProperty("forecast")
    private double forecast;

    @JsonProperty("target")
    private double target;

    public JsonGlobalNetPositions() {
    }

    public double getForecast() {
        return forecast;
    }

    public double getTarget() {
        return target;
    }
}

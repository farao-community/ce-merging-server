/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonInRegionNetPositions {

    @JsonProperty("initial")
    private double initial;

    @JsonProperty("initialMin")
    private double initialMin;

    @JsonProperty("initialMax")
    private double initialMax;

    @JsonProperty("finalMin")
    private double finalMin;

    @JsonProperty("finalMax")
    private double finalMax;

    @JsonProperty("forecast")
    private double forecast;

    @JsonProperty("target")
    private double target;

    public double getInitial() {
        return initial;
    }

    public double getInitialMin() {
        return initialMin;
    }

    public double getInitialMax() {
        return initialMax;
    }

    public double getFinalMin() {
        return finalMin;
    }

    public double getFinalMax() {
        return finalMax;
    }

    public double getForecast() {
        return forecast;
    }

    public double getTarget() {
        return target;
    }
}

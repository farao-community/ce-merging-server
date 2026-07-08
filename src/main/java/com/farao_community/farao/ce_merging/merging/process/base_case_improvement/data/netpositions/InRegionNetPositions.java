/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions;

import com.farao_community.farao.ce_merging.common.serialize.DoubleSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public record InRegionNetPositions(@JsonSerialize(using = DoubleSerializer.class) double initial,
                                   @JsonSerialize(using = DoubleSerializer.class) double initialMin,
                                   @JsonSerialize(using = DoubleSerializer.class) double initialMax,
                                   @JsonSerialize(using = DoubleSerializer.class) double finalMin,
                                   @JsonSerialize(using = DoubleSerializer.class) double finalMax,
                                   @JsonSerialize(using = DoubleSerializer.class) double forecast,
                                   @JsonSerialize(using = DoubleSerializer.class) double target) {
    @JsonCreator
    public InRegionNetPositions(@JsonProperty("initial") double initial,
                                @JsonProperty("initialMin") double initialMin,
                                @JsonProperty("initialMax") double initialMax,
                                @JsonProperty("finalMin") double finalMin,
                                @JsonProperty("finalMax") double finalMax,
                                @JsonProperty("forecast") double forecast,
                                @JsonProperty("target") double target) {
        this.initial = initial;
        this.initialMin = initialMin;
        this.initialMax = initialMax;
        this.finalMin = finalMin;
        this.finalMax = finalMax;
        this.forecast = forecast;
        this.target = target;
    }
}

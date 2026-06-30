/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process.result;

import com.farao_community.farao.ce_merging.common.serialize.DoubleSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class BciAreaResults {
    private final InRegionNetPositions inRegionNetPositions;
    private final GlobalNetPositions globalNetPositions;
    private Boolean bciApplied;

    @JsonCreator
    public BciAreaResults(@JsonProperty("inRegionNetPositions") final InRegionNetPositions inRegionNetPositions,
                          @JsonProperty("globalNetPositions") final GlobalNetPositions globalNetPositions,
                          @JsonProperty("bciApplied") final Boolean bciApplied) {
        this.inRegionNetPositions = inRegionNetPositions;
        this.globalNetPositions = globalNetPositions;
        this.bciApplied = bciApplied;
    }

    public Boolean getBciApplied() {
        return bciApplied;
    }

    public void setBciApplied(final Boolean bciApplied) {
        this.bciApplied = bciApplied;
    }

    public InRegionNetPositions getInRegionNetPositions() {
        return inRegionNetPositions;
    }

    public GlobalNetPositions getGlobalNetPositions() {
        return globalNetPositions;
    }

    public static class InRegionNetPositions {
        @JsonSerialize(using = DoubleSerializer.class)
        private double initial;

        @JsonSerialize(using = DoubleSerializer.class)
        private double initialMin;

        @JsonSerialize(using = DoubleSerializer.class)
        private double initialMax;

        @JsonSerialize(using = DoubleSerializer.class)
        private double finalMin;

        @JsonSerialize(using = DoubleSerializer.class)
        private double finalMax;

        @JsonSerialize(using = DoubleSerializer.class)
        private double forecast;

        @JsonSerialize(using = DoubleSerializer.class)
        private double target;

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

    public static class GlobalNetPositions {
        @JsonSerialize(using = DoubleSerializer.class)
        private double forecast;

        @JsonSerialize(using = DoubleSerializer.class)
        private double target;

        @JsonCreator
        public GlobalNetPositions(@JsonProperty("forecast") double forecast, @JsonProperty("target") double target) {
            this.forecast = forecast;
            this.target = target;
        }

        public double getForecast() {
            return forecast;
        }

        public double getTarget() {
            return target;
        }
    }

}

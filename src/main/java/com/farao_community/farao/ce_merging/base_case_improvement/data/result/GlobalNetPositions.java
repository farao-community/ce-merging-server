package com.farao_community.farao.ce_merging.base_case_improvement.data.result;

import com.farao_community.farao.ce_merging.common.serialize.DoubleSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class GlobalNetPositions {
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

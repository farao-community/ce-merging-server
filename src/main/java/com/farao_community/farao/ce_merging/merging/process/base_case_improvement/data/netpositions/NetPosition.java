package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record NetPosition(double withVirtualHubs, double withoutVirtualHubs) {
    @JsonCreator
    public NetPosition(@JsonProperty("withVirtualHubs") final double withVirtualHubs,
                       @JsonProperty("withoutVirtualHubs") final double withoutVirtualHubs) {
        this.withVirtualHubs = withVirtualHubs;
        this.withoutVirtualHubs = withoutVirtualHubs;
    }
}

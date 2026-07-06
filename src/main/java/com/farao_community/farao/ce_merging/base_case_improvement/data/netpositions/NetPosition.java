package com.farao_community.farao.ce_merging.base_case_improvement.data.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class NetPosition {
    final double withVirtualHubs;
    final double withoutVirtualHubs;

    @JsonCreator
    public NetPosition(@JsonProperty("withVirtualHubs") final double withVirtualHubs,
                       @JsonProperty("withoutVirtualHubs") final double withoutVirtualHubs) {
        this.withVirtualHubs = withVirtualHubs;
        this.withoutVirtualHubs = withoutVirtualHubs;
    }

    public double getWithVirtualHubs() {
        return withVirtualHubs;
    }

    public double getWithoutVirtualHubs() {
        return withoutVirtualHubs;
    }
}

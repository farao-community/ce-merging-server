/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.model.netpositions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class NetPositionsValues {
    private double withVirtualHubs;
    private double withoutVirtualHubs;

    public NetPositionsValues(@JsonProperty("withVirtualHubs") final double withVirtualHubs,
                              @JsonProperty("withoutVirtualHubs") final double withoutVirtualHubs) {
        this.withVirtualHubs = withVirtualHubs;
        this.withoutVirtualHubs = withoutVirtualHubs;
    }

    public double getWithVirtualHubs() {
        return withVirtualHubs;
    }

    public void setWithVirtualHubs(double withVirtualHubs) {
        this.withVirtualHubs = withVirtualHubs;
    }

    public double getWithoutVirtualHubs() {
        return withoutVirtualHubs;
    }

    public void setWithoutVirtualHubs(double withoutVirtualHubs) {
        this.withoutVirtualHubs = withoutVirtualHubs;
    }
}

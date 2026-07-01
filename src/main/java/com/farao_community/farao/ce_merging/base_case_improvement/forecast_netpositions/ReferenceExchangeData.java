/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.forecast_netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ReferenceExchangeData {
    private String areaOutId;
    private String areaInId;
    private double flow;

    /**
     * @param areaOutId id of origin area
     * @param areaInId  id of destination area
     * @param flow      flow exchanged from origin area to destination area in MW
     */
    @JsonCreator
    public ReferenceExchangeData(@JsonProperty("areaOutId") final String areaOutId,
                                 @JsonProperty("areaInId") final String areaInId,
                                 @JsonProperty("flow") final double flow) {
        this.areaOutId = Objects.requireNonNull(areaOutId);
        this.areaInId = Objects.requireNonNull(areaInId);
        this.flow = flow;
    }

    public String getAreaOutId() {
        return areaOutId;
    }

    public String getAreaInId() {
        return areaInId;
    }

    public double getFlow() {
        return flow;
    }

    public void setAreaOutId(final String areaOutId) {
        this.areaOutId = areaOutId;
    }

    public void setAreaInId(final String areaInId) {
        this.areaInId = areaInId;
    }

    public void setFlow(final double flow) {
        this.flow = flow;
    }

    boolean flowsBetween(final String areaOutId, final String areaInId) {
        return goesTo(areaInId) && comesFrom(areaOutId);
    }

    public boolean comesFrom(final String areaId) {
        return areaOutId.equals(areaId);
    }

    public boolean goesTo(final String areaId) {
        return areaInId.equals(areaId);
    }
}

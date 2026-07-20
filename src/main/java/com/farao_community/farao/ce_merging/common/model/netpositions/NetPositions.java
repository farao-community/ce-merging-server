/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.model.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class NetPositions {
    private final NetPositionsValues globalNetPosition;
    private final NetPositionsValues inRegionNetPosition;
    private double outBciNetPosition;
    private final Map<String, Double> virtualHubsExchanges;
    private final Map<String, Double> globalDetailedExchanges;
    private final GenerationAndLoadQuantity generationAndLoadQuantity;

    @JsonCreator
    public NetPositions(@JsonProperty("globalNetPosition") final NetPositionsValues globalNetPosition,
                        @JsonProperty("inRegionNetPosition") final NetPositionsValues inRegionNetPosition,
                        @JsonProperty("outBciNetPosition") final double outBciNetPosition,
                        @JsonProperty("virtualHubsExchanges") final Map<String, Double> virtualHubsExchanges,
                        @JsonProperty("globalDetailedExchanges") final Map<String, Double> globalDetailedExchanges,
                        @JsonProperty("generationAndLoadQuantity") final GenerationAndLoadQuantity generationAndLoadQuantity) {
        this.globalNetPosition = globalNetPosition;
        this.inRegionNetPosition = inRegionNetPosition;
        this.outBciNetPosition = outBciNetPosition;
        this.virtualHubsExchanges = virtualHubsExchanges;
        this.globalDetailedExchanges = globalDetailedExchanges;
        this.generationAndLoadQuantity = generationAndLoadQuantity;
    }

    public NetPositionsValues getGlobalNetPosition() {
        return globalNetPosition;
    }

    public NetPositionsValues getInRegionNetPosition() {
        return inRegionNetPosition;
    }

    public double getOutBciNetPosition() {
        return outBciNetPosition;
    }

    public void setOutBciNetPosition(double outBciNetPosition) {
        this.outBciNetPosition = outBciNetPosition;
    }

    public Map<String, Double> getVirtualHubsExchanges() {
        return virtualHubsExchanges;
    }

    public double getVirtualHubFlow(final String virtualHub) {
        return virtualHubsExchanges.getOrDefault(virtualHub, .0);
    }

    public GenerationAndLoadQuantity getGenerationAndLoadQuantity() {
        return generationAndLoadQuantity;
    }

    public Map<String, Double> getGlobalDetailedExchanges() {
        return globalDetailedExchanges;
    }

}

package com.farao_community.farao.ce_merging.base_case_improvement.initial_netpositions.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public final class CountryNetPositions {
    final NetPosition globalNetPosition;
    final NetPosition inRegionNetPosition;
    final double outBciNetPosition;
    final Map<String, Double> globalDetailedVirtualHubsExchanges;
    final Map<String, Double> globalDetailedExchanges;
    final GenerationAndLoadQuantity generationAndLoadQuantity;

    @JsonCreator
    public CountryNetPositions(@JsonProperty("globalNetPosition") final NetPosition globalNetPosition,
                               @JsonProperty("inRegionNetPosition") final NetPosition inRegionNetPosition,
                               @JsonProperty("outBciNetPosition") final double outBciNetPosition,
                               @JsonProperty("virtualHubsExchanges") final Map<String, Double> globalDetailedVirtualHubsExchanges,
                               @JsonProperty("globalDetailedExchanges") final Map<String, Double> globalDetailedExchanges,
                               @JsonProperty("generationAndLoadQuantity") final GenerationAndLoadQuantity generationAndLoadQuantity) {
        this.globalNetPosition = globalNetPosition;
        this.inRegionNetPosition = inRegionNetPosition;
        this.outBciNetPosition = outBciNetPosition;
        this.globalDetailedVirtualHubsExchanges = globalDetailedVirtualHubsExchanges;
        this.generationAndLoadQuantity = generationAndLoadQuantity;
        this.globalDetailedExchanges = globalDetailedExchanges;
    }

    public NetPosition getGlobalNetPosition() {
        return globalNetPosition;
    }

    public NetPosition getInRegionNetPosition() {
        return inRegionNetPosition;
    }

    public double getOutBciNetPosition() {
        return outBciNetPosition;
    }

    public Map<String, Double> getGlobalDetailedVirtualHubsExchanges() {
        return globalDetailedVirtualHubsExchanges;
    }

    public GenerationAndLoadQuantity getGenerationAndLoadQuantity() {
        return generationAndLoadQuantity;
    }

    public Map<String, Double> getGlobalDetailedExchanges() {
        return globalDetailedExchanges;
    }

}

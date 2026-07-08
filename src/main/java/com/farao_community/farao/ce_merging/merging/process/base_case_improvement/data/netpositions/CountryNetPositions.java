package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CountryNetPositions(NetPosition globalNetPosition,
                                  NetPosition inRegionNetPosition,
                                  double outBciNetPosition,
                                  FlowByAreaMap globalDetailedVirtualHubsExchanges,
                                  FlowByAreaMap globalDetailedExchanges,
                                  GenerationAndLoadQuantity generationAndLoadQuantity) {
    @JsonCreator
    public CountryNetPositions(@JsonProperty("globalNetPosition") final NetPosition globalNetPosition,
                               @JsonProperty("inRegionNetPosition") final NetPosition inRegionNetPosition,
                               @JsonProperty("outBciNetPosition") final double outBciNetPosition,
                               @JsonProperty("virtualHubsExchanges") final FlowByAreaMap globalDetailedVirtualHubsExchanges,
                               @JsonProperty("globalDetailedExchanges") FlowByAreaMap globalDetailedExchanges,
                               @JsonProperty("generationAndLoadQuantity") final GenerationAndLoadQuantity generationAndLoadQuantity) {
        this.globalNetPosition = globalNetPosition;
        this.inRegionNetPosition = inRegionNetPosition;
        this.outBciNetPosition = outBciNetPosition;
        this.globalDetailedVirtualHubsExchanges = globalDetailedVirtualHubsExchanges;
        this.generationAndLoadQuantity = generationAndLoadQuantity;
        this.globalDetailedExchanges = globalDetailedExchanges;
    }

}

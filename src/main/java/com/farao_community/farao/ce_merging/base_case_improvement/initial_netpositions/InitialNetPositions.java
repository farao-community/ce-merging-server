/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.initial_netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public final class InitialNetPositions {

    private final Map<String, CountryNetPositions> countryNetPositionsMap;

    @JsonCreator
    public InitialNetPositions(@JsonProperty("netPositionsByCountryMap") final Map<String, CountryNetPositions> countryNetPositionsMap) {
        this.countryNetPositionsMap = countryNetPositionsMap;
    }

    Map<String, CountryNetPositions> getCountryNetPositionsMap() {
        return countryNetPositionsMap;
    }

    public static final class CountryNetPositions {
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

        NetPosition getGlobalNetPosition() {
            return globalNetPosition;
        }

        NetPosition getInRegionNetPosition() {
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

        public static final class NetPosition {
            final double withVirtualHubs;
            final double withoutVirtualHubs;

            @JsonCreator
            public NetPosition(@JsonProperty("withVirtualHubs") final double withVirtualHubs,
                               @JsonProperty("withoutVirtualHubs") final double withoutVirtualHubs) {
                this.withVirtualHubs = withVirtualHubs;
                this.withoutVirtualHubs = withoutVirtualHubs;
            }

            double getWithVirtualHubs() {
                return withVirtualHubs;
            }

            double getWithoutVirtualHubs() {
                return withoutVirtualHubs;
            }
        }

        public static final class GenerationAndLoadQuantity {
            public final double generation;
            public final double load;

            public double getGeneration() {
                return generation;
            }

            public double getLoad() {
                return load;
            }

            @JsonCreator
            public GenerationAndLoadQuantity(@JsonProperty("generation") final double generation,
                                             @JsonProperty("load") final double load) {
                this.generation = generation;
                this.load = load;
            }

        }

    }

}

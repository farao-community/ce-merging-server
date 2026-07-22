package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerationAndLoadQuantity(double generation, double load) {

    @JsonCreator
    public GenerationAndLoadQuantity(@JsonProperty("generation") final double generation,
                                     @JsonProperty("load") final double load) {
        this.generation = generation;
        this.load = load;
    }

}

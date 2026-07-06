package com.farao_community.farao.ce_merging.base_case_improvement.data.netpositions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class GenerationAndLoadQuantity {
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

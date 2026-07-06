/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class BciComputationResult {
    private final boolean bciActive;
    private final boolean bciFeasibilityRangesExtended;
    private final Map<String, BciAreaResults> bciResults;

    @JsonCreator
    public BciComputationResult(@JsonProperty("bciActive") final boolean bciActive,
                                @JsonProperty("bciFeasibilityRangesExtended") final boolean bciFeasibilityRangesExtended,
                                @JsonProperty("results") final Map<String, BciAreaResults> bciResultsByAreaId) {
        this.bciActive = bciActive;
        this.bciFeasibilityRangesExtended = bciFeasibilityRangesExtended;
        this.bciResults = bciResultsByAreaId;
    }

    public boolean isBciActive() {
        return bciActive;
    }

    public boolean isBciFeasibilityRangesExtended() {
        return bciFeasibilityRangesExtended;
    }

    public Map<String, BciAreaResults> getBciResults() {
        return bciResults;
    }
}

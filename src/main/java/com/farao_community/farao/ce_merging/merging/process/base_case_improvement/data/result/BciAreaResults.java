/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.GlobalNetPositions;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.InRegionNetPositions;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BciAreaResults {
    private final InRegionNetPositions inRegionNetPositions;
    private final GlobalNetPositions globalNetPositions;
    private Boolean bciApplied;

    @JsonCreator
    public BciAreaResults(@JsonProperty("inRegionNetPositions") final InRegionNetPositions inRegionNetPositions,
                          @JsonProperty("globalNetPositions") final GlobalNetPositions globalNetPositions,
                          @JsonProperty("bciApplied") final Boolean bciApplied) {
        this.inRegionNetPositions = inRegionNetPositions;
        this.globalNetPositions = globalNetPositions;
        this.bciApplied = bciApplied;
    }

    public Boolean getBciApplied() {
        return bciApplied;
    }

    public void setBciApplied(final Boolean bciApplied) {
        this.bciApplied = bciApplied;
    }

    public InRegionNetPositions getInRegionNetPositions() {
        return inRegionNetPositions;
    }

    public GlobalNetPositions getGlobalNetPositions() {
        return globalNetPositions;
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.last_loadflow;

import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class LastLoadFlowResult {

    private final LoadFlowOutput loadFlowOutput;
    private final NetPositionsResults netPositionsResults;

    @JsonCreator
    public LastLoadFlowResult(@JsonProperty("loadflowResults") final LoadFlowOutput loadFlowOutput,
                              @JsonProperty("netPositionsByCountryMap") final NetPositionsResults netPositionsResults) {
        this.loadFlowOutput = loadFlowOutput;
        this.netPositionsResults = netPositionsResults;
    }

    public LoadFlowOutput getLoadFlowResults() {
        return loadFlowOutput;
    }

    public NetPositionsResults getNetPositionsResults() {
        return netPositionsResults;
    }
}

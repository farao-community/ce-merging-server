/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonBciResult {

    @JsonProperty("inRegionNetPositions")
    private JsonInRegionNetPositions jsonInRegionNetPositions;

    @JsonProperty("globalNetPositions")
    private JsonGlobalNetPositions jsonGlobalNetPositions;

    @JsonProperty("bciApplied")
    private boolean bciApplied;

    public JsonBciResult() {
    }

    public JsonInRegionNetPositions getJsonInRegionNetPositions() {
        return jsonInRegionNetPositions;
    }

    public JsonGlobalNetPositions getJsonGlobalNetPositions() {
        return jsonGlobalNetPositions;
    }

    public boolean getBciApplied() {
        return bciApplied;
    }
}

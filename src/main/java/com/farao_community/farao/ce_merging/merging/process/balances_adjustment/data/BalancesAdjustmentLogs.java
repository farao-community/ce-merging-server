/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BalancesAdjustmentLogs(String status, int iterationNumber, String loadflowMode) {
    @JsonCreator
    public BalancesAdjustmentLogs(@JsonProperty("status") final String status,
                                  @JsonProperty("iterationNumber") final int iterationNumber,
                                  @JsonProperty("loadflowMode") final String loadflowMode) {
        this.status = status;
        this.iterationNumber = iterationNumber;
        this.loadflowMode = loadflowMode;
    }

}

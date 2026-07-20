/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Terminal;

import java.util.Optional;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;

public class Flow {

    @JsonProperty("flowIGM")
    private double flowIGM;
    @JsonProperty("flowCGM")
    private double flowCGM;

    public Flow() {
        this.flowIGM = NaN;
        this.flowCGM = NaN;
    }

    @JsonIgnore
    public void setIgmFlowFromBranch(final Branch<?> branch) {
        this.flowIGM = getBranchFlow(branch);
    }

    @JsonIgnore
    public void setCgmFlowFromBranch(final Branch<?> branch) {
        this.flowCGM = getBranchFlow(branch);
    }

    private double getBranchFlow(final Branch<?> branch) {
        final double p1 = Optional.ofNullable(branch).map(Branch::getTerminal1).map(Terminal::getP).orElse(NaN);
        final double p2 = Optional.ofNullable(branch).map(Branch::getTerminal2).map(Terminal::getP).orElse(NaN);

        return isNaN(p1) || isNaN(p2) ? 0 : (p1 - p2) / 2;
    }

    /*-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    ACCESSORS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public double getFlowIGM() {
        return flowIGM;
    }

    public double getFlowCGM() {
        return flowCGM;
    }

    public void setFlowIGM(double flowIGM) {
        this.flowIGM = flowIGM;
    }

    public void setFlowCGM(double flowCGM) {
        this.flowCGM = flowCGM;
    }
}

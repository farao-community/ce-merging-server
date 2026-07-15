package com.farao_community.farao.ce_merging.merging.process.pst_special_process.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.Branch;

import static java.lang.Double.NaN;

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
    public void setFlowIgmFrom(final Branch<?> branch) {
        this.flowIGM = getBranchFlow(branch);
    }

    @JsonIgnore
    public void setFlowCgmFrom(final Branch<?> branch) {
        this.flowCGM = getBranchFlow(branch);
    }

    private double getBranchFlow(final Branch<?> branch) {
        return branch != null && !Double.isNaN(branch.getTerminal1().getP()) && !Double.isNaN(branch.getTerminal2().getP())
            ? (branch.getTerminal1().getP() - branch.getTerminal2().getP()) / 2 : 0;
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

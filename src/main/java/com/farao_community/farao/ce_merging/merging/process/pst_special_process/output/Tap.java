package com.farao_community.farao.ce_merging.merging.process.pst_special_process.output;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.iidm.network.TwoWindingsTransformer;

import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInOutage;
import static java.lang.Double.NaN;

public class Tap {

    private static final int NEUTRAL_TAP = 0;

    @JsonProperty("tapIGM")
    private double tapIGM;
    @JsonProperty("tapCGM")
    private double tapCGM;

    public Tap() {
        this.tapIGM = 0;
        this.tapCGM = 0;
    }

    @JsonIgnore
    public void setTapIgmFrom(final TwoWindingsTransformer twt) {
       this.tapIGM = getTapValue(twt);
    }

    @JsonIgnore
    public void setTapCgmFrom(final TwoWindingsTransformer twt) {
        this.tapCGM = getTapValue(twt);
    }

    private double getTapValue(final TwoWindingsTransformer twt) {
        if (!isInOutage(twt) && twt.getPhaseTapChanger() != null) {
            return twt.getPhaseTapChanger().getTapPosition();
        } else if (twt != null && isInOutage(twt)) {
            return NEUTRAL_TAP;
        } else {
            return NaN;
        }
    }

    /*-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
                    ACCESSORS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    public double getTapIGM() {
        return tapIGM;
    }

    public double getTapCGM() {
        return tapCGM;
    }

    public void setTapIGM(double tapIGM) {
        this.tapIGM = tapIGM;
    }

    public void setTapCGM(double tapCGM) {
        this.tapCGM = tapCGM;
    }

}

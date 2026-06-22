/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MergedInformation {
    private XnodeStatus status;
    private double p;
    private double q;
    private double v1;
    private double v2;

    @JsonCreator
    public MergedInformation(@JsonProperty("status") XnodeStatus status, @JsonProperty("p") double p, @JsonProperty("q") double q, @JsonProperty("v1") double v1, @JsonProperty("v2") double v2) {
        this.status = status;
        this.p = p;
        this.q = q;
        this.v1 = v1;
        this.v2 = v2;
    }

    public XnodeStatus getStatus() {
        return status;
    }

    public void setStatus(XnodeStatus status) {
        this.status = status;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public double getQ() {
        return q;
    }

    public void setQ(double q) {
        this.q = q;
    }

    public double getV1() {
        return v1;
    }

    public void setV1(double v1) {
        this.v1 = v1;
    }

    public double getV2() {
        return v2;
    }

    public void setV2(double v2) {
        this.v2 = v2;
    }
}

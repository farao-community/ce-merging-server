/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AreaInformation {
    private String country;
    private XnodeStatus status;
    private String node;
    private double p;
    private double q;
    private double v;

    @JsonCreator
    public AreaInformation(@JsonProperty("country") String country, @JsonProperty("status") XnodeStatus status, @JsonProperty("node") String node, @JsonProperty("p") double p, @JsonProperty("q") double q, @JsonProperty("v") double v) {
        this.country = country;
        this.status = status;
        this.node = node;
        this.p = p;
        this.q = q;
        this.v = v;
    }

    public AreaInformation(String country, XnodeStatus status) {
        this(country, status, "", 0, 0, 0);
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public XnodeStatus getStatus() {
        return status;
    }

    public void setStatus(XnodeStatus status) {
        this.status = status;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
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

    public double getV() {
        return v;
    }

    public void setV(double v) {
        this.v = v;
    }
}

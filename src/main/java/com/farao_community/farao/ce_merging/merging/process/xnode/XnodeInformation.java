/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"area1Information", "area2Information", "mergedInformation"})
public class XnodeInformation {
    private AreaInformation area1Information;
    private AreaInformation area2Information;
    private MergedXnodeInformation mergedXnodeInformation;

    @JsonCreator
    public XnodeInformation(@JsonProperty("area1Information") AreaInformation area1Information, @JsonProperty("area2Information") AreaInformation area2Information, @JsonProperty("mergedInformation") MergedXnodeInformation mergedXnodeInformation) {
        this.area1Information = area1Information;
        this.area2Information = area2Information;
        this.mergedXnodeInformation = mergedXnodeInformation;
    }

    public XnodeInformation(AreaInformation area1Information, AreaInformation area2Information) {
        this(area1Information, area2Information, null);
    }

    public XnodeInformation(AreaInformation xnodeConnection1) {
        this(xnodeConnection1, null);
    }

    public AreaInformation getArea1Information() {
        return area1Information;
    }

    public void setArea1Information(AreaInformation area1Information) {
        this.area1Information = area1Information;
    }

    public AreaInformation getArea2Information() {
        return area2Information;
    }

    public void setArea2Information(AreaInformation area2Information) {
        this.area2Information = area2Information;
    }

    public MergedXnodeInformation getMergedInformation() {
        return mergedXnodeInformation;
    }

    public void setMergedInformation(MergedXnodeInformation mergedXnodeInformation) {
        this.mergedXnodeInformation = mergedXnodeInformation;
    }

    @JsonIgnore
    public boolean isIncorrect() {
        return area1Information != null && area2Information != null && area1Information.getStatus() != area2Information.getStatus();
    }

    @JsonIgnore
    public boolean isIncomplete() {
        return area1Information == null || area2Information == null;
    }
}

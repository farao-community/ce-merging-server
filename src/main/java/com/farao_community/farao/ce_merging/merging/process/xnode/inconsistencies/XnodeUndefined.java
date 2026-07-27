/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies;

import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class XnodeUndefined {
    private String name;
    private String nodeName;
    private String countryPresent;
    private XnodeStatus status;

    public XnodeUndefined() {
    }

    public XnodeUndefined(String name, String nodeName, String countryPresent, XnodeStatus status) {
        this.name = name;
        this.nodeName = nodeName;
        this.countryPresent = countryPresent;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getCountryPresent() {
        return countryPresent;
    }

    public void setCountryPresent(String countryPresent) {
        this.countryPresent = countryPresent;
    }

    public XnodeStatus getStatus() {
        return status;
    }

    public void setStatus(XnodeStatus status) {
        this.status = status;
    }
}

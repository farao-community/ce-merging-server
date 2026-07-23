/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode;

import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus;

public class XnodeUndefined {
    private String name;
    private String nodeName;
    private String countryPresent;
    private XnodeStatus status;

    public XnodeUndefined() {
    }

    public XnodeUndefined(final String name, final String nodeName, final String countryPresent, final XnodeStatus status) {
        this.name = name;
        this.nodeName = nodeName;
        this.countryPresent = countryPresent;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getCountryPresent() {
        return countryPresent;
    }

    public void setCountryPresent(final String countryPresent) {
        this.countryPresent = countryPresent;
    }

    public XnodeStatus getStatus() {
        return status;
    }

    public void setStatus(final XnodeStatus status) {
        this.status = status;
    }
}

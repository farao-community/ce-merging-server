/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies;

import com.farao_community.farao.ce_merging.merging.process.xnode.AreaInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus;

public class XnodeIncomplete {
    private String name;
    private String nodeName;
    private String countryPresent;
    private String countryAbsent;
    private XnodeStatus status;

    public XnodeIncomplete(final String name, final AreaInformation existingInfo, final String absent) {
        this.name = name;
        this.nodeName = existingInfo.getNode();
        this.countryPresent = existingInfo.getCountry();
        this.countryAbsent = absent;
        this.status = existingInfo.getStatus();
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

    public String getCountryAbsent() {
        return countryAbsent;
    }

    public void setCountryAbsent(final String countryAbsent) {
        this.countryAbsent = countryAbsent;
    }

    public XnodeStatus getStatus() {
        return status;
    }

    public void setStatus(final XnodeStatus status) {
        this.status = status;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 */

package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

public class XnodesCheck {
    @JsonPropertyOrder(alphabetic = true)
    private final Map<String, XnodeInformation> xnodeInformationMap;

    @JsonCreator
    public XnodesCheck(@JsonProperty("XnodeInformationMap") Map<String, XnodeInformation> xnodeInformationMap) {
        this.xnodeInformationMap = xnodeInformationMap;
    }

    public Map<String, XnodeInformation> getXnodeInformationMap() {
        return xnodeInformationMap;
    }

}

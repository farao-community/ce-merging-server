/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.json;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JsonHvdcAlignmentConfiguration {

    private List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignment;
    private List<ZeroFlowNodeDto> setZeroFlowNodes;
    private List<String> dkHvdcXnodes;
    private String defaultSlackNode;

    @JsonCreator
    public JsonHvdcAlignmentConfiguration(@JsonProperty("hvdcXNodeAlignment") List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignment,
                                          @JsonProperty("setZeroFlowNodes") List<ZeroFlowNodeDto> setZeroFlowNodes,
                                          @JsonProperty("dkHvdcXnodes") List<String> dkHvdcXnodes,
                                          @JsonProperty("defaultSlackNode") String defaultSlackNode) {
        this.hvdcXNodeAlignment = Optional.ofNullable(hvdcXNodeAlignment).orElse(new ArrayList<>());
        this.setZeroFlowNodes =  Optional.ofNullable(setZeroFlowNodes).orElse(new ArrayList<>());
        this.dkHvdcXnodes =  Optional.ofNullable(dkHvdcXnodes).orElse(new ArrayList<>());
        this.defaultSlackNode = defaultSlackNode;
    }

    public List<HvdcAlignmentXNodeCoupleDto> getHvdcXNodeAlignment() {
        return hvdcXNodeAlignment;
    }

    public void setHvdcXNodeAlignment(final List<HvdcAlignmentXNodeCoupleDto> hvdcXNodeAlignment) {
        this.hvdcXNodeAlignment = hvdcXNodeAlignment;
    }

    public List<ZeroFlowNodeDto> getSetZeroFlowNodes() {
        return setZeroFlowNodes;
    }

    public void setSetZeroFlowNodes(final List<ZeroFlowNodeDto> setZeroFlowNodes) {
        this.setZeroFlowNodes = setZeroFlowNodes;
    }

    public List<String> getDkHvdcXnodes() {
        return dkHvdcXnodes;
    }

    public void setDkHvdcXnodes(final List<String> dkHvdcXnodes) {
        this.dkHvdcXnodes = dkHvdcXnodes;
    }

    public String getDefaultSlackNode() {
        return defaultSlackNode;
    }

    public void setDefaultSlackNode(final String defaultSlackNode) {
        this.defaultSlackNode = defaultSlackNode;
    }
}


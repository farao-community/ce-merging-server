/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.ListUtils.deNulledList;

@Data
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
        this.hvdcXNodeAlignment = deNulledList(hvdcXNodeAlignment);
        this.setZeroFlowNodes =  deNulledList(setZeroFlowNodes);
        this.dkHvdcXnodes =  deNulledList(dkHvdcXnodes);
        this.defaultSlackNode = defaultSlackNode;
    }
}


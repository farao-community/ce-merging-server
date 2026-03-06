/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.hvdc_alignment;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

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
        this.hvdcXNodeAlignment = Optional.ofNullable(hvdcXNodeAlignment).orElse(emptyList());
        this.setZeroFlowNodes =  Optional.ofNullable(setZeroFlowNodes).orElse(emptyList());
        this.dkHvdcXnodes =  Optional.ofNullable(dkHvdcXnodes).orElse(emptyList());
        this.defaultSlackNode = defaultSlackNode;
    }
}


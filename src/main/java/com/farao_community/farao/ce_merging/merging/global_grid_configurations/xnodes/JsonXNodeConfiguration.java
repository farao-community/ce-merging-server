/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.xnodes;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;

import java.util.List;

@Data
public class JsonXNodeConfiguration {

    private List<XnodeDto> xNodesList;

    @JsonCreator
    public JsonXNodeConfiguration(final List<XnodeDto> xNodesList) {
        this.xNodesList = xNodesList;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.json;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeDto;
import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class JsonXNodeConfiguration {

    private List<XnodeDto> xNodesList;

    @JsonCreator
    public JsonXNodeConfiguration(final List<XnodeDto> xNodesList) {
        this.xNodesList = xNodesList;
    }

    public JsonXNodeConfiguration() {
    }

    public List<XnodeDto> getXNodesList() {
        return xNodesList;
    }

    public void setXNodesList(final List<XnodeDto> xNodesList) {
        this.xNodesList = xNodesList;
    }
}

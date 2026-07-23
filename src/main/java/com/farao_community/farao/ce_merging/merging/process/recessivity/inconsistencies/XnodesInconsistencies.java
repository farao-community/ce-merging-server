/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class XnodesInconsistencies {
    private List<XnodeIncorrect> xnodeIncorrectList;
    private List<XnodeIncomplete> xnodeIncompleteList;

    public XnodesInconsistencies() {
        xnodeIncompleteList = new ArrayList<>();
        xnodeIncorrectList = new ArrayList<>();
    }

    @JsonCreator
    public XnodesInconsistencies(@JsonProperty("xnodeIncorrectList") final List<XnodeIncorrect> xnodeIncoherentList,
                                 @JsonProperty("xnodeIncompleteList") final List<XnodeIncomplete> xnodeIncompleteList) {
        this.xnodeIncorrectList = xnodeIncoherentList;
        this.xnodeIncompleteList = xnodeIncompleteList;
    }

    public List<XnodeIncorrect> getXnodeIncorrectList() {
        return xnodeIncorrectList;
    }

    public void setXnodeIncorrectList(final List<XnodeIncorrect> xnodeIncorrectList) {
        this.xnodeIncorrectList = xnodeIncorrectList;
    }

    public List<XnodeIncomplete> getXnodeIncompleteList() {
        return xnodeIncompleteList;
    }

    public void setXnodeIncompleteList(final List<XnodeIncomplete> xnodeIncompleteList) {
        this.xnodeIncompleteList = xnodeIncompleteList;
    }


}

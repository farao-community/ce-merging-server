/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode.XnodeIncomplete;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode.XnodeIncorrect;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistent_xnode.XnodeUndefined;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class XnodesInconsistencies {
    private List<XnodeIncorrect> xnodeIncorrectList;
    private List<XnodeIncomplete> xnodeIncompleteList;
    private List<XnodeUndefined> xnodeUndefinedList;

    public XnodesInconsistencies() {
    }

    @JsonCreator
    public XnodesInconsistencies(@JsonProperty("xnodeIncorrectList") final List<XnodeIncorrect> xnodeIncoherentList,
                                 @JsonProperty("xnodeIncompleteList") final List<XnodeIncomplete> xnodeIncompleteList,
                                 @JsonProperty("xnodeUndefinedList") final List<XnodeUndefined> xnodeUndefinedList) {
        this.xnodeIncorrectList = xnodeIncoherentList;
        this.xnodeIncompleteList = xnodeIncompleteList;
        this.xnodeUndefinedList = xnodeUndefinedList;
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

    public List<XnodeUndefined> getXnodeUndefinedList() {
        return xnodeUndefinedList;
    }

    public void setXnodeUndefinedList(final List<XnodeUndefined> xnodeUndefinedList) {
        this.xnodeUndefinedList = xnodeUndefinedList;
    }

}

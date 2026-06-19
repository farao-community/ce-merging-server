/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class HvdcAlignmentXNodeCouple implements Serializable {
    private String referenceXNode;
    private String recessiveXNode;

    public String getReferenceXNode() {
        return referenceXNode;
    }

    public void setReferenceXNode(final String referenceXNode) {
        this.referenceXNode = referenceXNode;
    }

    public String getRecessiveXNode() {
        return recessiveXNode;
    }

    public void setRecessiveXNode(final String recessiveXNode) {
        this.recessiveXNode = recessiveXNode;
    }
}

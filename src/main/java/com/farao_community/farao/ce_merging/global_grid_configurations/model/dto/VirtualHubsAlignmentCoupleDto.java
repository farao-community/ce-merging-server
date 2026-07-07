/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractXNodeCouple;
import jakarta.persistence.Embeddable;

@Embeddable
public class VirtualHubsAlignmentCoupleDto extends AbstractXNodeCouple {

    public VirtualHubsAlignmentCoupleDto(final String referenceXNode, final String recessiveXNode) {
        this.referenceXNode = referenceXNode;
        this.recessiveXNode = recessiveXNode;
    }

    public VirtualHubsAlignmentCoupleDto() {
    }
}

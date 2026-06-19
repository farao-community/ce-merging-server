/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes;

import jakarta.persistence.Embeddable;

@Embeddable
public class XnodeDto extends Xnode {
    public XnodeDto(final String name,
                    final String area1,
                    final String area2,
                    final String subarea1,
                    final String subarea2) {
        this.name = name;
        this.area1 = area1;
        this.area2 = area2;
        this.subarea1 = subarea1;
        this.subarea2 = subarea2;
    }

    public XnodeDto() {

    }
}

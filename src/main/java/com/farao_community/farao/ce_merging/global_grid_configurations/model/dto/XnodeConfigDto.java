/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractXNodeConfig;

import com.farao_community.farao.ce_merging.xsd.xnodes.Xnode;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class XnodeConfigDto extends AbstractXNodeConfig implements Serializable {

    public XnodeConfigDto() {
    }

    public XnodeConfigDto(final String name,
                       final String area1,
                       final String subarea1,
                       final String area2,
                       final String subarea2) {
        this.name = name;
        this.area1 = area1;
        this.area2 = area2;
        this.subarea1 = subarea1;
        this.subarea2 = subarea2;
    }

    public static XnodeConfigDto fromXNodeEntity(final Xnode xnode) {
        final XnodeConfigDto dto = new XnodeConfigDto();
        dto.setName(xnode.getName());
        dto.setArea1(xnode.getArea1());
        dto.setArea2(xnode.getArea2());
        dto.setSubarea1(xnode.getSubarea1());
        dto.setSubarea2(xnode.getSubarea2());
        return dto;
    }
}

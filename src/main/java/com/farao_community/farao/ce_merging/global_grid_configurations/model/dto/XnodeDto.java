/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.xsd.Xnode;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class XnodeDto implements Serializable {
    private String name;
    private String area1;
    private String area2;
    private String subarea1;
    private String subarea2;

    public static XnodeDto fromXNodeEntity(final Xnode xnode) {
        final XnodeDto dto = new XnodeDto();
        dto.setName(xnode.getName());
        dto.setArea1(xnode.getArea1());
        dto.setArea2(xnode.getArea2());
        dto.setSubarea1(xnode.getSubarea1());
        dto.setSubarea2(xnode.getSubarea2());
        return dto;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getArea1() {
        return area1;
    }

    public void setArea1(final String area1) {
        this.area1 = area1;
    }

    public String getArea2() {
        return area2;
    }

    public void setArea2(final String area2) {
        this.area2 = area2;
    }

    public String getSubarea1() {
        return subarea1;
    }

    public void setSubarea1(final String subarea1) {
        this.subarea1 = subarea1;
    }

    public String getSubarea2() {
        return subarea2;
    }

    public void setSubarea2(final String subarea2) {
        this.subarea2 = subarea2;
    }
}

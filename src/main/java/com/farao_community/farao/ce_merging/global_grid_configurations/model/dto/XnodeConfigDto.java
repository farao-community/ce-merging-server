/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.farao_community.farao.ce_merging.xsd.xnodes.Xnode;
import com.powsybl.iidm.network.Country;
import jakarta.persistence.Embeddable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

@Embeddable
public class XnodeConfigDto implements Serializable {

    private String name;
    private String area1;
    private String area2;
    private String subarea1;
    private String subarea2;

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

    public boolean isArea1Tso(final String tso) {
        return isAreaOfTso(area1, subarea1, tso);
    }

    public boolean isArea2Tso(final String tso) {
        return isAreaOfTso(area2, subarea2, tso);
    }

    private boolean isAreaOfTso(final String area,
                                final String subarea,
                                final String tso) {
        if (GermanTso.includes(tso)) {
            return tso.equals(subarea) && Country.valueOf(area) == DE;
        } else if (DANISH_TSO.equals(tso)) {
            return tso.equals(subarea) && Country.valueOf(area) == DK;
        } else {
            return tso.equals(area);
        }
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

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

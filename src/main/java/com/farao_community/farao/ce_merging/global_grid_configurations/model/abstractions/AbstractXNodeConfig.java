/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;

import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.Country;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

@MappedSuperclass
public abstract class AbstractXNodeConfig {
    protected String name;
    protected String area1;
    protected String area2;
    protected String subarea1;
    protected String subarea2;

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

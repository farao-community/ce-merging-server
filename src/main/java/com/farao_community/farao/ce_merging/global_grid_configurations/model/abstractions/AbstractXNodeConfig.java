/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@MappedSuperclass
public abstract class AbstractXNodeConfig {
    protected String name;
    protected String area1;
    protected String area2;
    protected String subarea1;
    protected String subarea2;

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

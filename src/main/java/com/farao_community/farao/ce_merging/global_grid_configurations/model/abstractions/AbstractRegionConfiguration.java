/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MappedSuperclass;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.FetchType.LAZY;

@MappedSuperclass
public abstract class AbstractRegionConfiguration<T extends AbstractTsoInfos> {

    protected String id;
    protected String name;
    @ElementCollection(fetch = LAZY)
    protected Map<String, String> areasIn;
    @ElementCollection(fetch = LAZY)
    protected Map<String, String> areasOut;
    @ElementCollection(fetch = LAZY)
    protected Map<String, T> germanyZone;

    @JsonIgnore
    public Map<String, String> getAreasAll() {
        final Map<String, String> areasAllMap = new HashMap<>();
        areasAllMap.putAll(areasIn);
        areasAllMap.putAll(areasOut);
        return areasAllMap;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Map<String, String> getAreasIn() {
        return areasIn;
    }

    public void setAreasIn(final Map<String, String> areasIn) {
        this.areasIn = areasIn;
    }

    public Map<String, String> getAreasOut() {
        return areasOut;
    }

    public void setAreasOut(final Map<String, String> areasOut) {
        this.areasOut = areasOut;
    }

    public Map<String, T> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, T> germanyZone) {
        this.germanyZone = germanyZone;
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

    @JsonIgnore
    public String getAreaInEic(final String eic) {
        return areasIn.get(eic);
    }
}

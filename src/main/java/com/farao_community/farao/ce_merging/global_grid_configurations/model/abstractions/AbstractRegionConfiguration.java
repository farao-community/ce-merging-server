/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import java.util.Map;

import static jakarta.persistence.FetchType.LAZY;

@MappedSuperclass
public abstract class AbstractRegionConfiguration {
    @Id
    protected Long ref;
    protected String id;
    protected String name;
    @ElementCollection(fetch = LAZY)
    protected Map<String, String> areasIn;
    @ElementCollection(fetch = LAZY)
    protected Map<String, String> areasOut;
    @OneToMany
    protected Map<String, TsoInfos> germanyZone;

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
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

    public Map<String, TsoInfos> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, TsoInfos> germanyZone) {
        this.germanyZone = germanyZone;
    }
}

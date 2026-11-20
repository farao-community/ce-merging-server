/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashMap;
import java.util.Map;

public class RegionConfigurationDto {

    private Long ref;

    private String name;

    protected String id;

    protected Map<String, String> areasIn = new HashMap<>();

    protected Map<String, String> areasOut = new HashMap<>();

    @JsonProperty("germanyZones")
    protected Map<String, TsoInfosDto> germanyZone;

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
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

    public Map<String, TsoInfosDto> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, TsoInfosDto> germanyZone) {
        this.germanyZone = germanyZone;
    }

    @JsonIgnore
    public Map<String, String> getAreasAll() {
        Map<String, String> areasAllMap = new HashMap<>();
        areasAllMap.putAll(areasIn);
        areasAllMap.putAll(areasOut);
        return areasAllMap;
    }

    @JsonIgnore
    public Map<String, String> getCountriesByEicCode() {
        BiMap<String, String> biMapAreas = HashBiMap.create(getAreasAll());
        return biMapAreas.inverse();
    }

    @JsonIgnore
    public String getAreaInEic(final String eic) {
        return areasIn.get(eic);
    }
}

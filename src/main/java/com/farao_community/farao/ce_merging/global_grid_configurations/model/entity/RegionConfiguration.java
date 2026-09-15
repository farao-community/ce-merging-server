/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfiguration")
public class RegionConfiguration implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConfiguration.class);
    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    protected Map<String, TsoInfos> germanyZone;

    private String id;
    private String name;
    @ElementCollection(fetch = LAZY)
    private Map<String, String> areasIn = new HashMap<>();
    @ElementCollection(fetch = LAZY)
    private Map<String, String> areasOut = new HashMap<>();

    public Map<String, TsoInfos> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, TsoInfos> germanyZone) {
        this.germanyZone = germanyZone;
    }

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error during json parse regions configuration");
            throw new ServiceIOException("Error during json parse regions configuration", e);
        }
    }

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

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
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

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NAME;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfiguration")
public class RegionConfiguration implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConfiguration.class);

    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @Column(name = NAME)
    private String name;

    @Column(name = ID)
    private String id;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_areasin_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasin_name")
    @Column(name = "areasin_eic")
    private Map<String, String> areasIn;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_areasout_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasout_name")
    @Column(name = "areasout_eic")
    private Map<String, String> areasOut;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    private Map<String, TsoInfos> germanyZone;

    public RegionConfiguration(final long ref,
                               final String name,
                               final String id,
                               final Map<String, String> areasIn,
                               final Map<String, String> areasOut,
                               final Map<String, TsoInfos> germanyZone) {
        this.ref = ref;
        this.name = name;
        this.id = id;
        this.areasIn = areasIn;
        this.areasOut = areasOut;
        this.germanyZone = germanyZone;
    }

    public RegionConfiguration() {

    }

    public Long getRef() {
        return ref;
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

    public Map<String, TsoInfos> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, TsoInfos> germanyZone) {
        this.germanyZone = germanyZone;
    }

    @JsonIgnore
    public Map<String, String> getAllAreas() {
        final Map<String, String> areasAllMap = new HashMap<>();
        areasAllMap.putAll(areasIn);
        areasAllMap.putAll(areasOut);
        return areasAllMap;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (final JsonProcessingException e) {
            final String errorMsg = "Error during JSON parsing of regions configuration";
            LOGGER.error(errorMsg);
            throw new ServiceIOException(errorMsg, e);
        }
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractRegionConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfigurationdto")
public class RegionConfigurationDto extends AbstractRegionConfiguration<TsoInfosDto> {

    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @Column(name = "name")
    private String name;

    @Column(name = "id")
    protected String id;

    @ElementCollection
    @CollectionTable(name = "regionconfigurationdto_areasin_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfigurationdto_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasin_name")
    @Column(name = "areasin_eic")
    protected Map<String, String> areasIn = new HashMap<>();

    @ElementCollection
    @CollectionTable(name = "regionconfigurationdto_areasout_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfigurationdto_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasout_name")
    @Column(name = "areasout_eic")
    protected Map<String, String> areasOut = new HashMap<>();

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
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

}

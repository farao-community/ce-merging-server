/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.region_eic;

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
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfigurationdto")
@Data
public class RegionConfigurationDto {

    @Id
    @GeneratedValue(strategy = AUTO)
    private long ref;

    @Column(name = "name")
    private String name;

    @Column(name = "id")
    private String id;

    @ElementCollection
    @CollectionTable(name = "regionconfigurationdto_ariasin_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfigurationdto_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "ariasin_name")
    @Column(name = "ariasin_eic")
    private Map<String, String> areasIn;

    @ElementCollection
    @CollectionTable(name = "regionconfigurationdto_ariasout_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfigurationdto_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "ariasout_name")
    @Column(name = "ariasout_eic")
    private Map<String, String> areasOut;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    private Map<String, TsoInfosDto> germanyZone;

    @JsonIgnore
    public Map<String, String> getAreasAll() {
        Map<String, String> areasAllMap = new HashMap<>();
        areasAllMap.putAll(areasIn);
        areasAllMap.putAll(areasOut);
        return areasAllMap;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new ServiceIOException("Error during json parse regions configuration", e);
        }
    }

}

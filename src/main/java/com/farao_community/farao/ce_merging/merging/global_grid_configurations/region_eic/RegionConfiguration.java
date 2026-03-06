/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfiguration")
@Getter
@Setter
@Slf4j
public class RegionConfiguration {
    @Id
    @GeneratedValue(strategy = AUTO)
    private long ref;

    @Column(name = "name")
    private String name;

    @Column(name = "id")
    private String id;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_ariasin_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "ariasin_name")
    @Column(name = "ariasin_eic")
    private Map<String, String> areasIn;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_ariasout_code_mapping",
            joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "ariasout_name")
    @Column(name = "ariasout_eic")
    private Map<String, String> areasOut;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    private Map<String, TsoInfos> germanyZone;

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
            final String errorMsg = "Error during json parse regions configuration";
            log.error(errorMsg);
            throw new ServiceIOException(errorMsg, e);
        }
    }
}

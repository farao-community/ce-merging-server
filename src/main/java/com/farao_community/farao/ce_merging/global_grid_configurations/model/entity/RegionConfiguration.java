/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractRegionConfiguration;
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
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfiguration")
public class RegionConfiguration extends AbstractRegionConfiguration<TsoInfos> implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionConfiguration.class);
    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @Column(name = "name")
    private String name;

    @Column(name = "id")
    private String id;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_areasin_code_mapping",
        joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasin_name")
    @Column(name = "areasin_eic")
    protected Map<String, String> areasIn;

    @ElementCollection
    @CollectionTable(name = "regionconfiguration_areasout_code_mapping",
        joinColumns = {@JoinColumn(name = "regionconfiguration_ref", referencedColumnName = "ref")})
    @MapKeyColumn(name = "areasout_name")
    @Column(name = "areasout_eic")
    protected Map<String, String> areasOut;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    private Map<String, TsoInfos> germanyZone;

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

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }
}

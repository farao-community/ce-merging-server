/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractRegionConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
@Table(name = "regionconfigurationdto")
public class RegionConfigurationDto extends AbstractRegionConfiguration<TsoInfosDto> {

    @Id
    @GeneratedValue(strategy = AUTO)
    private Long ref;

    @OneToMany(cascade = ALL)
    @JsonProperty(value = "germanyZones")
    protected Map<String, TsoInfosDto> germanyZone;

    public Map<String, TsoInfosDto> getGermanyZone() {
        return germanyZone;
    }

    public void setGermanyZone(final Map<String, TsoInfosDto> germanyZone) {
        this.germanyZone = germanyZone;
    }

    public Long getRef() {
        return ref;
    }

    public void setRef(final Long ref) {
        this.ref = ref;
    }
}

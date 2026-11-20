/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

import static jakarta.persistence.CascadeType.ALL;

@Entity
public class RegionConfigurationRecord {
    @Id
    protected String id;
    protected LocalDateTime validFrom;
    protected LocalDateTime validTo;
    protected LocalDateTime publishedOn;
    @OneToOne(cascade = ALL)
    private RegionConfiguration regionConfiguration;

    public RegionConfigurationRecord(final String id,
                                     final LocalDateTime validFrom,
                                     final LocalDateTime validTo,
                                     final LocalDateTime publishedOn,
                                     final RegionConfiguration regionConfiguration) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
        this.regionConfiguration = regionConfiguration;
    }

    public RegionConfigurationRecord() {
        this.regionConfiguration = new RegionConfiguration();
    }

    public RegionConfiguration getRegionConfiguration() {
        return regionConfiguration;
    }

    public void setRegionConfiguration(final RegionConfiguration regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(final LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getPublishedOn() {
        return publishedOn;
    }

    public void setPublishedOn(final LocalDateTime publishedOn) {
        this.publishedOn = publishedOn;
    }
}

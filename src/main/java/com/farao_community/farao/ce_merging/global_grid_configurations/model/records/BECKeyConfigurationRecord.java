/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
public class BECKeyConfigurationRecord {
    @Id
    protected String id;
    protected LocalDateTime validFrom;
    protected LocalDateTime validTo;
    protected LocalDateTime publishedOn;
    @OneToMany(cascade = ALL)
    private List<BecByBoundary> becMatrix = new ArrayList<>();

    public BECKeyConfigurationRecord(final String id,
                                     final LocalDateTime validFrom,
                                     final LocalDateTime validTo,
                                     final LocalDateTime publishedOn,
                                     final List<BecByBoundary> becMatrix) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
        this.becMatrix = becMatrix;
    }

    public BECKeyConfigurationRecord() {
    }

    public List<BecByBoundary> getBecMatrix() {
        return becMatrix;
    }

    public void setBecMatrix(final List<BecByBoundary> becMatrix) {
        this.becMatrix = becMatrix;
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

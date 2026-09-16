/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ID;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
public class BecByBoundaryDto {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Basic(optional = false)
    @Column(name = ID, unique = true, nullable = false)
    private Long id;

    @Embedded
    private BorderDto border;

    @ElementCollection(fetch = LAZY)
    private List<BecCoefficientsDto> coefficientByCountry;

    public BecByBoundaryDto(final BorderDto border,
                            final List<BecCoefficientsDto> coefficientByCountry) {
        this.border = border;
        this.coefficientByCountry = coefficientByCountry;
    }

    public BecByBoundaryDto() {

    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public BorderDto getBorder() {
        return border;
    }

    public void setBorder(final BorderDto border) {
        this.border = border;
    }

    public List<BecCoefficientsDto> getCoefficientByCountry() {
        return coefficientByCountry;
    }

    public void setCoefficientByCountry(final List<BecCoefficientsDto> coefficientByCountry) {
        this.coefficientByCountry = coefficientByCountry;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

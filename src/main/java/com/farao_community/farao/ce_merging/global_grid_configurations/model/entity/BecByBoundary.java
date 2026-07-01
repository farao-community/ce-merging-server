/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.List;

@Entity
public class BecByBoundary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id", unique = true, nullable = false)
    private long id;

    @Embedded
    private Border border;

    @ElementCollection(fetch = FetchType.LAZY)
    private List<BecCoefficients> coefficientByCountry;

    public BecByBoundary(Border border, List<BecCoefficients> coefficientByCountry) {
        this.border = border;
        this.coefficientByCountry = coefficientByCountry;
    }

    public BecByBoundary() {
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    public List<BecCoefficients> getCoefficientByCountry() {
        return coefficientByCountry;
    }

    public void setCoefficientByCountry(List<BecCoefficients> coefficientByCountry) {
        this.coefficientByCountry = coefficientByCountry;
    }
}

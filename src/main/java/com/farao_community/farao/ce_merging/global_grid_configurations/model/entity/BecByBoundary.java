/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractBecByBoundary;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
public class BecByBoundary extends AbstractBecByBoundary implements Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Basic(optional = false)
    @Column(name = "id", unique = true, nullable = false)
    private Long id;

    @Embedded
    private Border border;

    @ElementCollection(fetch = LAZY)
    private List<BecCoefficients> coefficientByCountry;

    public BecByBoundary(final Border border, final List<BecCoefficients> coefficientByCountry) {
        this.border = border;
        this.coefficientByCountry = coefficientByCountry;
    }

    public BecByBoundary() {
    }

}

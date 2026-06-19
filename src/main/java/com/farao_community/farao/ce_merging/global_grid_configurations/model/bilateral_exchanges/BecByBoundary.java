/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ID;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
public class BecByBoundary implements Serializable {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Basic(optional = false)
    @Column(name = ID, unique = true, nullable = false)
    private long id;

    @Embedded
    private Border border;

    @ElementCollection(fetch = LAZY)
    private List<BecCoefficients> coefficientByCountry;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(final Border border) {
        this.border = border;
    }

    public List<BecCoefficients> getCoefficientByCountry() {
        return coefficientByCountry;
    }

    public void setCoefficientByCountry(final List<BecCoefficients> coefficientByCountry) {
        this.coefficientByCountry = coefficientByCountry;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@MappedSuperclass
public abstract class AbstractBecByBoundary<T extends AbstractBecCoefficients> {
    @Id
    protected Long id;
    protected Border border;
    @ElementCollection(fetch = LAZY)
    protected List<T> coefficientByCountry;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Border getBorder() {
        return border;
    }

    public void setBorder(final Border border) {
        this.border = border;
    }

    public List<T> getCoefficientByCountry() {
        return coefficientByCountry;
    }

    public void setCoefficientByCountry(final List<T> coefficientByCountry) {
        this.coefficientByCountry = coefficientByCountry;
    }
}

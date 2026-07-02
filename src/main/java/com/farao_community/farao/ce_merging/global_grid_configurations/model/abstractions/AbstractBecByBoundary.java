/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;

import java.util.List;

public abstract class AbstractBecByBoundary {
    protected Long id;
    protected Border border;
    protected List<? extends AbstractBecCoefficients> coefficientByCountry;

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

    public List<? extends AbstractBecCoefficients> getCoefficientByCountry() {
        return coefficientByCountry;
    }

    public void setCoefficientByCountry(final List<? extends AbstractBecCoefficients> coefficientByCountry) {
        this.coefficientByCountry = coefficientByCountry;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import jakarta.persistence.Embeddable;

@Embeddable
public class BecCoefficientsDto {
    private String countryCode;
    private Double coefficient;

    public BecCoefficientsDto(final String countryCode, final Double coefficient) {
        this.countryCode = countryCode;
        this.coefficient = coefficient;
    }

    public BecCoefficientsDto() {

    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(final String countryCode) {
        this.countryCode = countryCode;
    }

    public Double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(final Double coefficient) {
        this.coefficient = coefficient;
    }
}

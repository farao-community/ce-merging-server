/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractBecCoefficients;
import jakarta.persistence.Embeddable;

@Embeddable
public class BecCoefficientsDto extends AbstractBecCoefficients {

    public BecCoefficientsDto(final String countryCode, final Double coefficient) {
        this.countryCode = countryCode;
        this.coefficient = coefficient;
    }

    public BecCoefficientsDto() {

    }
}

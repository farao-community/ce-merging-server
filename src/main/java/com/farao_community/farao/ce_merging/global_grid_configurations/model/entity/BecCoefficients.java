/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.entity;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.abstractions.AbstractBecCoefficients;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class BecCoefficients extends AbstractBecCoefficients implements Serializable {

    public BecCoefficients(final String countryCode, final double coefficient) {
        this.countryCode = countryCode;
        this.coefficient = coefficient;
    }

    public BecCoefficients() {
    }

}

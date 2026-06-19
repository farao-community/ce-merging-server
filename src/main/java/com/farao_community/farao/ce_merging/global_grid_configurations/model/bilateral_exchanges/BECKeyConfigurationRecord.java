/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.AbstractGridConfigurationRecord;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@Entity
public class BECKeyConfigurationRecord extends AbstractGridConfigurationRecord {
    @OneToMany(cascade = ALL)
    private List<BecByBoundaryDto> becMatrix = new ArrayList<>();

    public BECKeyConfigurationRecord(final String id,
                                     final LocalDateTime validFrom,
                                     final LocalDateTime validTo,
                                     final LocalDateTime publishedOn,
                                     final List<BecByBoundaryDto> becMatrix) {
        this.id = id;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.publishedOn = publishedOn;
        this.becMatrix = becMatrix;
    }

    public BECKeyConfigurationRecord() {

    }

    public List<BecByBoundaryDto> getBecMatrix() {
        return becMatrix;
    }

    public void setBecMatrix(final List<BecByBoundaryDto> becMatrix) {
        this.becMatrix = becMatrix;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.io.Serializable;

import static jakarta.persistence.CascadeType.ALL;

/**
 * WARNING: this class is linked to the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Embeddable
@Data
public class DailyInputs implements Serializable {

    @OneToOne(cascade = ALL)
    private SavedFile mergingRequest = new SavedFile();

    public void setMergingRequestFilePath(final String mergingRequestFilePath) {
        mergingRequest.feedPathAndName(mergingRequestFilePath);
    }
}

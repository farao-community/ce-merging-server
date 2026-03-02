/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Data
@Embeddable
public class Configurations implements Serializable {

    /**
     * virtual hubs list
     */
    @ElementCollection(fetch = LAZY)
    private List<VirtualHubRecord> virtualHubList = new ArrayList<>();
    /**
     * Border direction list
     */
    @ElementCollection(fetch = LAZY)
    private List<BorderDirectionRecord> borderDirectionRecords = new ArrayList<>();
    /**
     * The dc load flow parameters file
     */
    @OneToOne(cascade = ALL)
    private SavedFile dcLoadFlowParameters = new SavedFile();
    /**
     * The ac load flow parameters file
     */
    @OneToOne(cascade = ALL)
    private SavedFile acLoadFlowParameters = new SavedFile();
    /**
     * The basecase improvement parameters file
     */
    @OneToOne(cascade = ALL)
    private SavedFile baseCaseImprovementParameters = new SavedFile();
    /**
     * The balances adjustment parameters file
     */
    @OneToOne(cascade = ALL)
    private SavedFile balancesAdjustmentParameters = new SavedFile();

}

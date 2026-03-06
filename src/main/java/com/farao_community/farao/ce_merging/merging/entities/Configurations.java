/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities;

import com.farao_community.farao.ce_merging.global_grid_configurations.bilateral_exchanges.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment.HvdcAlignmentXNodeCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.region_eic.RegionConfiguration;
import com.powsybl.loadflow.LoadFlowParameters;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
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

    /**
     * Xnode config list
     */
    @ElementCollection(fetch = LAZY)
    private List<Xnode> xnodeList = new ArrayList<>();

    /**
     * The SavedFile of the recessivity parameters file
     */
    @OneToOne(cascade = ALL)
    private SavedFile recessivityParameters = new SavedFile();

    /**
     * The BEC keys configuration
     */
    @OneToMany(cascade = ALL)
    private List<BecByBoundary> becMatrixConfig = new ArrayList<>();

    /**
     * The region Configuration
     */
    @OneToOne(cascade = ALL)
    private RegionConfiguration regionConfiguration;

    /**
     * reference/recessive country couple for hvdc alignment
     */
    @ElementCollection(fetch = LAZY)
    private List<HvdcAlignmentXNodeCouple> virtualHubsAlignmentCouples = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<ZeroFlowNode> zeroFlowNodes = new ArrayList<>();

    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    private transient LoadFlowParameters loadFlowParameters;

}

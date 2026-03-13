/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.powsybl.loadflow.LoadFlowParameters;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;

@Getter
@Setter
@NoArgsConstructor
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
    private SavedFile basecaseImprovementParameters = new SavedFile();
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

    @ElementCollection(fetch = LAZY)
    private List<String> dkHvdcXnodes = new ArrayList<>();

    private String defaultSlackNode;

    private transient LoadFlowParameters loadFlowParameters;

    // tagged falsely unused in IntelliJ - used for configuration deserialization

    public void setAcLoadFlowParametersFilePath(final String filePath) {
        acLoadFlowParameters.feedPathAndName(filePath);
    }

    public void setBalancesAdjustmentParametersFilePath(final String filePath) {
        balancesAdjustmentParameters.feedPathAndName(filePath);
    }

    public void  setBasecaseImprovementParametersFilePath(final String filePath) {
        basecaseImprovementParameters.feedPathAndName(filePath);
    }

    public void setRecessivityParametersFilePath(final String filePath) {
        recessivityParameters.feedPathAndName(filePath);
    }

    public void setDcLoadFlowParametersFilePath(final String filePath) {
        dcLoadFlowParameters.feedPathAndName(filePath);
    }

}

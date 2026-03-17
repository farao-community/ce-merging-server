/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import java.io.Serializable;

public class ConfigurationsDto implements Serializable {
    /**
     * The location of the dc load flow parameters
     */
    private String dcLoadFlowParametersLocation;
    /**
     * The location of the ac load flow parameters
     */
    private String acLoadFlowParametersLocation;
    /**
     * The location of the base cas improvement parameters
     */
    private String basecaseImprovementParametersLocation;
    /**
     * The location of the balances adjustment parameters
     */
    private String balancesAdjustmentParametersLocation;

    /**
     * The location of the recessivity parameters
     */
    private String recessivityParametersLocation;

    /**
     * The BEC keys configuration
     */
    private String becConfig;

    public String getBalancesAdjustmentParametersLocation() {
        return balancesAdjustmentParametersLocation;
    }

    public void setBalancesAdjustmentParametersLocation(final String balancesAdjustmentParametersLocation) {
        this.balancesAdjustmentParametersLocation = balancesAdjustmentParametersLocation;
    }

    public String getDcLoadFlowParametersLocation() {
        return dcLoadFlowParametersLocation;
    }

    public void setDcLoadFlowParametersLocation(final String dcLoadFlowParametersLocation) {
        this.dcLoadFlowParametersLocation = dcLoadFlowParametersLocation;
    }

    public String getAcLoadFlowParametersLocation() {
        return acLoadFlowParametersLocation;
    }

    public void setAcLoadFlowParametersLocation(final String acLoadFlowParametersLocation) {
        this.acLoadFlowParametersLocation = acLoadFlowParametersLocation;
    }

    public String getBasecaseImprovementParametersLocation() {
        return basecaseImprovementParametersLocation;
    }

    public void setBasecaseImprovementParametersLocation(final String basecaseImprovementParametersLocation) {
        this.basecaseImprovementParametersLocation = basecaseImprovementParametersLocation;
    }

    public String getRecessivityParametersLocation() {
        return recessivityParametersLocation;
    }

    public void setRecessivityParametersLocation(final String recessivityParametersLocation) {
        this.recessivityParametersLocation = recessivityParametersLocation;
    }

    public String getBecConfig() {
        return becConfig;
    }

    public void setBecConfig(final String becConfig) {
        this.becConfig = becConfig;
    }
}

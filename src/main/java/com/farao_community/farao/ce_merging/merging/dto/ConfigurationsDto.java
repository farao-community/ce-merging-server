/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic.RegionConfiguration;

import java.io.Serializable;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
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

    /**
     * The EIC Codes configuration
     */
    private RegionConfiguration regionConfiguration;

    public String getDcLoadFlowParametersLocation() {
        return dcLoadFlowParametersLocation;
    }

    public void setDcLoadFlowParametersLocation(String dcLoadFlowParametersLocation) {
        this.dcLoadFlowParametersLocation = dcLoadFlowParametersLocation;
    }

    public String getAcLoadFlowParametersLocation() {
        return acLoadFlowParametersLocation;
    }

    public void setAcLoadFlowParametersLocation(String acLoadFlowParametersLocation) {
        this.acLoadFlowParametersLocation = acLoadFlowParametersLocation;
    }

    public String getBasecaseImprovementParametersLocation() {
        return basecaseImprovementParametersLocation;
    }

    public void setBasecaseImprovementParametersLocation(String basecaseImprovementParametersLocation) {
        this.basecaseImprovementParametersLocation = basecaseImprovementParametersLocation;
    }

    public String getBalancesAdjustmentParametersLocation() {
        return balancesAdjustmentParametersLocation;
    }

    public void setBalancesAdjustmentParametersLocation(String balancesAdjustmentParametersLocation) {
        this.balancesAdjustmentParametersLocation = balancesAdjustmentParametersLocation;
    }

    public String getRecessivityParametersLocation() {
        return recessivityParametersLocation;
    }

    public void setRecessivityParametersLocation(String recessivityParametersLocation) {
        this.recessivityParametersLocation = recessivityParametersLocation;
    }

    public String getBecConfig() {
        return becConfig;
    }

    public void setBecConfig(String becConfig) {
        this.becConfig = becConfig;
    }

    public RegionConfiguration getRegionConfiguration() {
        return regionConfiguration;
    }

    public void setRegionConfiguration(RegionConfiguration regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }
}

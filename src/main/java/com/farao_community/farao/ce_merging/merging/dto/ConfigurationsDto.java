/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic.RegionConfiguration;
import lombok.Data;

import java.io.Serializable;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Data
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

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.dto;

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

}

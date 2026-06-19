/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BecCoefficients;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BecCoefficientsDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.Border;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BorderDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.HvdcAlignmentXNodeCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.TsoInfos;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.TsoInfosDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes.Xnode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes.XnodeDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import test_utils.GetterSetterVerifier;

import java.util.List;

class GridConfigurationModelTest {
    static List<Class<?>> models = List.of(BecByBoundaryDto.class,
                                           BecByBoundary.class,
                                           BecCoefficients.class,
                                           BecCoefficientsDto.class,
                                           Border.class,
                                           BorderDto.class,
                                           HvdcAlignmentXNodeCouple.class,
                                           HvdcAlignmentXNodeCoupleDto.class,
                                           ZeroFlowNode.class,
                                           ZeroFlowNodeDto.class,
                                           RegionConfiguration.class,
                                           RegionConfigurationDto.class,
                                           TsoInfos.class,
                                           TsoInfosDto.class,
                                           Xnode.class,
                                           XnodeDto.class);

    @ParameterizedTest
    @FieldSource("models")
    void modelsShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

}

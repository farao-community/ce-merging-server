/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecCoefficientsDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BorderDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.TsoInfosDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import test_utils.GetterSetterVerifier;

import java.util.List;

class GridConfigurationModelTest {
    static List<Class<?>> dtos = List.of(BecByBoundaryDto.class,
                                         BecCoefficientsDto.class,
                                         BorderDto.class,
                                         HvdcAlignmentXNodeCoupleDto.class,
                                         ZeroFlowNodeDto.class,
                                         RegionConfigurationDto.class,
                                         TsoInfosDto.class,
                                         XnodeDto.class);

    @ParameterizedTest
    @FieldSource("dtos")
    void dtosShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

}

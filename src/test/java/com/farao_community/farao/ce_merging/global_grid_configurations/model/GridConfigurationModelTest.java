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
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.XnodeConfigDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecCoefficients;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.HvdcAlignmentXNodeCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonBecConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.AbstractGridConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
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
                                         XnodeConfigDto.class);

    static List<Class<?>> entities = List.of(BecByBoundary.class,
                                             BecCoefficients.class,
                                             Border.class,
                                             HvdcAlignmentXNodeCouple.class,
                                             ZeroFlowNode.class,
                                             RegionConfiguration.class,
                                             TsoInfos.class,
                                             XnodeConfig.class);

    static List<Class<? extends AbstractGridConfigurationRecord>> records = List.of(BECKeyConfigurationRecord.class,
                                                                                    RegionConfigurationRecord.class,
                                                                                    VirtualHubsConfigurationRecord.class,
                                                                                    XNodeConfigurationRecord.class);

    static List<Class<?>> jsonConfigs = List.of(JsonBecConfiguration.class,
                                                JsonHvdcAlignmentConfiguration.class,
                                                JsonRegionConfiguration.class,
                                                JsonXNodeConfiguration.class);

    @ParameterizedTest
    @FieldSource("dtos")
    void dtosShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }
    @ParameterizedTest
    @FieldSource("entities")
    void entitiesShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }


    @ParameterizedTest
    @FieldSource("records")
    void recordsShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

    @ParameterizedTest
    @FieldSource("jsonConfigs")
    void jsonConfigsShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

}

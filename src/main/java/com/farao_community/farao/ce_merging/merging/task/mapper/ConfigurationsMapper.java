/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.BecByBoundaryMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.BorderDirectionMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.HvdcAlignmentXNodeCoupleMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.RegionConfigurationMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.VirtualHubMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.XnodeMapper;
import com.farao_community.farao.ce_merging.global_grid_configurations.mapper.ZeroFlowNodeMapper;
import com.farao_community.farao.ce_merging.merging.task.dto.ConfigurationsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RegionConfigurationMapper.class, VirtualHubMapper.class, BorderDirectionMapper.class, XnodeMapper.class, BecByBoundaryMapper.class, HvdcAlignmentXNodeCoupleMapper.class, ZeroFlowNodeMapper.class})
public interface ConfigurationsMapper {
    @Mapping(source = "dcLoadFlowParameters.location", target = "dcLoadFlowParametersLocation")
    @Mapping(source = "acLoadFlowParameters.location", target = "acLoadFlowParametersLocation")
    @Mapping(source = "basecaseImprovementParameters.location", target = "basecaseImprovementParametersLocation")
    @Mapping(source = "balancesAdjustmentParameters.location", target = "balancesAdjustmentParametersLocation")
    @Mapping(source = "recessivityParameters.location", target = "recessivityParametersLocation")
    ConfigurationsDto configurationsToConfigurationsDto(final Configurations configurations);
}

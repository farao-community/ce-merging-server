/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.mapper;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ZeroFlowNodeMapper {
    List<ZeroFlowNode> mapToZeroFlowNodeList(List<ZeroFlowNodeDto> zeroFlowNodeDtoList);
}

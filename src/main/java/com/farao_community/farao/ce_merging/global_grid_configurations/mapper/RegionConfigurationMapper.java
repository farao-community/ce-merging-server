/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.mapper;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {TsoInfosMapper.class})
public interface RegionConfigurationMapper {
    // The DTO ref belongs to the global configuration table: the task gets its own rows
    @Mapping(target = "ref", ignore = true)
    RegionConfiguration mapToRegionConfiguration(RegionConfigurationDto regionConfigurationDto);
}

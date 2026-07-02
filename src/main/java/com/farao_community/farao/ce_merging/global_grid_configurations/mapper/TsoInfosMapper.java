/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.mapper;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.TsoInfosDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TsoInfosMapper {
    TsoInfos mapToTsoInfos(TsoInfosDto tsoInfosDto);
}

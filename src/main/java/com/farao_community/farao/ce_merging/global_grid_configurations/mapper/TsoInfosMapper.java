/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.mapper;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.TsoInfosDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TsoInfosMapper {
    TsoInfos mapToTsoInfos(TsoInfosDto tsoInfosDto);
}

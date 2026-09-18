/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.OutputsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OutputsMapper {

    @Mapping(source = "refProg.location", target = "refProgLocation")
    @Mapping(source = "cgm.location", target = "cgmLocation")
    @Mapping(source = "bciReport.location", target = "bciReportLocation")
    OutputsDto outputsToOutputsDto(Outputs outputs);
}

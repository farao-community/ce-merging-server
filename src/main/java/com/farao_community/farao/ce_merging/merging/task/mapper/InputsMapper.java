/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.IgmDto;
import com.farao_community.farao.ce_merging.merging.task.dto.InputsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InputsMapper {

    @Mapping(source = "generationLoadShiftKeys.location", target = "generationLoadShiftKeysLocation")
    @Mapping(source = "externalConstraints.location", target = "externalConstraintsLocation")
    @Mapping(source = "feasibilityRanges.location", target = "feasibilityRangesLocation")
    @Mapping(source = "dcLinks.location", target = "dcLinksLocation")
    @Mapping(source = "netPositionForecast.location", target = "netPositionForecastLocation")
    InputsDto inputsToInputsDto(Inputs inputs);

    @Mapping(source = "igmFile.location", target = "igmFileLocation")
    @Mapping(source = "igmQualityReportFile.location", target = "igmQualityReportLocation")
    IgmDto igmDataToIgmDto(IgmData igmData);
}

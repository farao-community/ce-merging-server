/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.mapper;


import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.openrao.virtualhubs.VirtualHub;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VirtualHubMapper {
    @Mappings({
        @Mapping(source = "relatedMa.code", target = "relatedMaCode"),
        @Mapping(source = "relatedMa.eic", target = "relatedMaEic")
    })
    VirtualHubRecord mapToVirtualHubRecord(VirtualHub virtualHub);

    List<VirtualHubRecord> mapToVirtualHubRecordList(List<VirtualHub> virtualHubList);
}

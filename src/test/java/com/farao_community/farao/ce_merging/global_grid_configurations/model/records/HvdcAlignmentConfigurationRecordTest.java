/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.HvdcAlignmentXNodeCoupleDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.ZeroFlowNodeDto;
import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HvdcAlignmentConfigurationRecordTest {
    @Test
    void listSettersShouldNotCopyListRefs() {
        GetterSetterVerifier.forClass(HvdcAlignmentConfigurationRecord.class)
            .exclude("hvdcXNodeAlignmentCouplesDto")
            .exclude("zeroFlowNodeDtos")
            .exclude("dkHvdcXnodes")
            .verify();

        final HvdcAlignmentConfigurationRecord configurationRecord = new HvdcAlignmentConfigurationRecord();
        final List<String> dkHvdcNodes = new ArrayList<>(List.of("a"));
        final List<HvdcAlignmentXNodeCoupleDto> couples = new ArrayList<>(List.of(new HvdcAlignmentXNodeCoupleDto()));
        final List<ZeroFlowNodeDto> zeroFlowNodes = new ArrayList<>(List.of(new ZeroFlowNodeDto()));
        configurationRecord.setDkHvdcXnodes(dkHvdcNodes);
        configurationRecord.setHvdcXNodeAlignmentCouplesDto(couples);
        configurationRecord.setZeroFlowNodeDtos(zeroFlowNodes);

        dkHvdcNodes.add("b");
        couples.add(new HvdcAlignmentXNodeCoupleDto());
        zeroFlowNodes.add(new ZeroFlowNodeDto());

        List.of(configurationRecord.getDkHvdcXnodes(),
                configurationRecord.getHvdcXNodeAlignmentCouplesDto(),
                configurationRecord.getZeroFlowNodeDtos())
            .forEach(list -> assertThat(list).hasSize(1));
    }

}

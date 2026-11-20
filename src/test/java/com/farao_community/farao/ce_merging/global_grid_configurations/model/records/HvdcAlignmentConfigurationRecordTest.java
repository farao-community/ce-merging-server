/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.records;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.VirtualHubsAlignmentCouple;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.ZeroFlowNode;
import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HvdcAlignmentConfigurationRecordTest {
    @Test
    void listSettersShouldNotCopyListRefs() {
        GetterSetterVerifier.forClass(HvdcAlignmentConfigurationRecord.class)
            .exclude("hvdcXNodeAlignmentCouples")
            .exclude("zeroFlowNodes")
            .exclude("dkHvdcXnodes")
            .verify();

        final HvdcAlignmentConfigurationRecord configurationRecord = new HvdcAlignmentConfigurationRecord();
        final List<String> dkHvdcNodes = new ArrayList<>(List.of("a"));
        final List<VirtualHubsAlignmentCouple> couples = new ArrayList<>(List.of(new VirtualHubsAlignmentCouple()));
        final List<ZeroFlowNode> zeroFlowNodes = new ArrayList<>(List.of(new ZeroFlowNode()));
        configurationRecord.setDkHvdcXnodes(dkHvdcNodes);
        configurationRecord.setHvdcXNodeAlignmentCouples(couples);
        configurationRecord.setZeroFlowNodes(zeroFlowNodes);

        dkHvdcNodes.add("b");
        couples.add(new VirtualHubsAlignmentCouple());
        zeroFlowNodes.add(new ZeroFlowNode());

        List.of(configurationRecord.getDkHvdcXnodes(),
                configurationRecord.getHvdcXNodeAlignmentCouples(),
                configurationRecord.getZeroFlowNodes())
            .forEach(list -> assertThat(list).hasSize(1));
    }

}

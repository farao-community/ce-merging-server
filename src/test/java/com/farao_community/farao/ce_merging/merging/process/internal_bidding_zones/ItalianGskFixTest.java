/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.util.JaxbUtils.readFromBytes;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianGskFix.handleItalianRegionsGlsk;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianGskFixTestData.EXPECTED_GSK_NODE_NAMES;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianGskFixTestData.getInputWithCountryBlock;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianGskFixTestData.getInputWithoutCountryBlock;
import static com.powsybl.iidm.network.Country.IT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItalianGskFixTest {

    private static final String ITALY_EIC = "10YIT-GRTN-----B";
    @Mock
    private MergingTask mergingTask;

    @Mock
    private Configurations taskConfiguration;

    @Mock
    private RegionConfiguration regionConfiguration;

    @BeforeEach
    void setupMergingTaskMocks() {
        when(mergingTask.getConfigurations()).thenReturn(taskConfiguration);
        when(taskConfiguration.getRegionConfiguration()).thenReturn(regionConfiguration);
        when(regionConfiguration.getAreasAll()).thenReturn(Map.of(IT.name(), ITALY_EIC));
        when(mergingTask.getTargetDate()).thenReturn(OffsetDateTime.parse("2026-10-23T12:00:00Z"));
    }

    @Test
    void shouldHandleGlskWithItaBzCountryBlocks() {
        final List<GSKSeriesType> resultSeries = getGskFixResult(getInputWithCountryBlock());
        assertHasMergedBiddingZones(resultSeries);
        assertThat(resultSeries.getFirst().getCountryGSKBlock()).hasSize(1);
    }

    @Test
    void shouldHandleGlskWithItaBzAutoAndManualBlocks() {
        final List<GSKSeriesType> resultSeries = getGskFixResult(getInputWithoutCountryBlock());
        assertHasMergedBiddingZones(resultSeries);
        final AutoGSKBlockType resultAutoBlock = resultSeries.getFirst().getAutoGSKBlock().getFirst();
        final List<String> resultNodeNames = resultAutoBlock.getAutoNodes().stream()
                .map(AutoNodesType::getNodeName)
                .map(IdentificationType::getV)
                .toList();
        assertThat(resultNodeNames)
                .hasSize(15)
                .containsOnlyOnceElementsOf(EXPECTED_GSK_NODE_NAMES);
    }

    private List<GSKSeriesType> getGskFixResult(final byte[] input) {
        return readFromBytes(GSKDocument.class, handleItalianRegionsGlsk(input, mergingTask))
                .getGSKSeries();
    }

    private void assertHasMergedBiddingZones(final List<GSKSeriesType> gskSeries) {
        assertThat(gskSeries).hasSize(1);
        final GSKSeriesType italySerie = gskSeries.getFirst();
        assertThat(italySerie.getArea().getV()).isEqualTo(ITALY_EIC);
    }

}

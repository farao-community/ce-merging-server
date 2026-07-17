/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_AREA_NAME_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_BALANCE_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_MISMATCH_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_TARGET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class BalancesAdjustmentSummaryTest {

    @Test
    void shouldLogSummaryByCountryWithData(final CapturedOutput capturedOutput) {
        Network network = Network.create("test", "test");

        ReportNode rootNode = mock(ReportNode.class);
        ReportNode iterationNode = mock(ReportNode.class);
        ReportNode mismatchNode = mock(ReportNode.class);
        ReportNode countryNode = mock(ReportNode.class);

        when(rootNode.getChildren()).thenReturn(List.of(iterationNode));

        TypedValue iterationValue = mock(TypedValue.class);
        when(iterationValue.toString()).thenReturn("0");
        when(iterationNode.getValue("iteration")).thenReturn(Optional.of(iterationValue));
        when(iterationNode.getChildren()).thenReturn(List.of(mismatchNode));

        when(mismatchNode.getMessageKey()).thenReturn(REPORT_NODE_MISMATCH_KEY);
        when(mismatchNode.getChildren()).thenReturn(List.of(countryNode));

        TypedValue areaNameValue = mock(TypedValue.class);
        when(areaNameValue.toString()).thenReturn(Country.FR.getName());
        when(countryNode.getValue(REPORT_NODE_AREA_NAME_KEY)).thenReturn(Optional.of(areaNameValue));

        TypedValue balanceValue = mock(TypedValue.class);
        when(balanceValue.toString()).thenReturn("100.0");
        when(countryNode.getValue(REPORT_NODE_BALANCE_KEY)).thenReturn(Optional.of(balanceValue));

        TypedValue targetValue = mock(TypedValue.class);
        when(targetValue.toString()).thenReturn("110.0");
        when(countryNode.getValue(REPORT_NODE_TARGET_KEY)).thenReturn(Optional.of(targetValue));

        TypedValue mismatchValue = mock(TypedValue.class);
        when(mismatchValue.toString()).thenReturn("10.0");
        when(countryNode.getValue(REPORT_NODE_MISMATCH_KEY)).thenReturn(Optional.of(mismatchValue));

        assertDoesNotThrow(() -> {
            BalancesAdjustmentSummary summary = new BalancesAdjustmentSummary(network, rootNode, 1);
            summary.logSummaryByCountry();
        });

        assertThat(capturedOutput)
            .contains("Incomplete shift for country FR : initial net position 0");
    }

}

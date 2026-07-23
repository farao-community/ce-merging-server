/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_AREA_NAME_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_BALANCE_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_MISMATCH_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentSummary.REPORT_NODE_TARGET_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.mockReportNode;
import static test_utils.CeTestUtils.mockTypedValue;

@ExtendWith(OutputCaptureExtension.class)
class BalancesAdjustmentSummaryTest {

    @Test
    void shouldLogCountrySummaryWithBalanceInformation(final CapturedOutput capturedOutput) {
        final Network network = Network.create("test", "test");

        final ReportNode countryNode = mockReportNode(Map.of(REPORT_NODE_AREA_NAME_KEY, mockTypedValue(Country.FR.getName()),
                                                             REPORT_NODE_BALANCE_KEY, mockTypedValue("100.0"),
                                                             REPORT_NODE_MISMATCH_KEY, mockTypedValue("10.0"),
                                                             REPORT_NODE_TARGET_KEY, mockTypedValue("110.0")));

        final ReportNode mismatchNode = mockReportNode(List.of(countryNode));
        when(mismatchNode.getMessageKey()).thenReturn(REPORT_NODE_MISMATCH_KEY);

        final ReportNode iterationNode = mockReportNode(List.of(mismatchNode),
                                                        Map.of("iteration", mockTypedValue("0")));

        new BalancesAdjustmentSummary(network, mockReportNode(List.of(iterationNode)), 1)
            .logSummaryByCountry();

        assertThat(capturedOutput)
            .contains("Incomplete shift for country FR : initial net position 0");
    }

}

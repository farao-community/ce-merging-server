/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.data;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_AREA_NAME_KEY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_BALANCE_KEY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_MISMATCH_KEY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_TARGET_KEY;
import static java.lang.Double.parseDouble;

public record CountryBalancesSummary(String areaName,
                                     double initial,
                                     double balance,
                                     double target,
                                     double mismatch) {

    public static CountryBalancesSummary from(final ReportNode reportNode, final double initialNetPosition) {
        return new CountryBalancesSummary(
            getString(reportNode, REPORT_NODE_AREA_NAME_KEY),
            initialNetPosition,
            getDouble(reportNode, REPORT_NODE_BALANCE_KEY),
            getDouble(reportNode, REPORT_NODE_TARGET_KEY),
            getDouble(reportNode, REPORT_NODE_MISMATCH_KEY)
        );
    }

    public static String getString(final ReportNode reportNode, final String key) {
        return reportNode.getValue(key)
            .map(TypedValue::toString)
            .orElseThrow(() -> new IllegalArgumentException("Missing value for key: %s".formatted(key)));
    }

    private static double getDouble(final ReportNode reportNode, final String key) {
        return parseDouble(getString(reportNode, key));
    }
}

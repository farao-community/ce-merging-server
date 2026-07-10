/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.data;

import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_AREA_NAME_KEY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_NODE_MISMATCH_KEY;
import static com.farao_community.farao.ce_merging.merging.process.balances_adjustment.data.CountryBalancesSummary.getString;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;

public class BalancesSummary {
    private static final Logger LOGGER = LoggerFactory.getLogger(BalancesSummary.class);
    private static final String ITERATION_KEY = "iteration";

    private final Map<Country, CountryBalancesSummary> summaryByCountry;

    public BalancesSummary(final Network network, final ReportNode reportNode, final int lastIteration) {
        this.summaryByCountry = getLastIterationNode(reportNode, lastIteration)
            .map(BalancesSummary::getMismatchChildNode)
            .map(mismatchReportNode -> reportNodeToSummaries(mismatchReportNode, network))
            .orElseGet(HashMap::new);
    }

    public void print() {
        summaryByCountry.forEach((country, summary) -> LOGGER.info(
            "Incomplete shift for country {} : initial net position {}, target net position {}, net position in last iteration {}",
            country, summary.initial(), summary.target(), summary.balance()
        ));
    }

    private Map<Country, CountryBalancesSummary> reportNodeToSummaries(final ReportNode reportNode, final Network network) {
        if (reportNode == null) {
            return new HashMap<>();
        }
        return reportNode.getChildren()
            .stream()
            .collect(toMap(BalancesSummary::getCountry,
                           childNode -> createSummaryForNode(childNode, network)));
    }

    private static Optional<ReportNode> getLastIterationNode(final ReportNode reportNode,
                                                             final int lastIteration) {
        return reportNode.getChildren()
            .stream()
            .filter(isNodeOfIteration(lastIteration))
            .findFirst();
    }

    private static Predicate<ReportNode> isNodeOfIteration(final int iteration) {
        return node -> parseInt(getString(node, ITERATION_KEY)) == iteration - 1;
    }

    private static ReportNode getMismatchChildNode(final ReportNode reportNode) {
        return reportNode.getChildren()
            .stream()
            .filter(childNode -> REPORT_NODE_MISMATCH_KEY.equals(childNode.getMessageKey()))
            .findFirst().orElse(null);
    }

    private static CountryBalancesSummary createSummaryForNode(final ReportNode reportNode, final Network network) {
        final double initialNetPosition = new CountryAreaFactory(getCountry(reportNode))
            .create(network)
            .getNetPosition();

        return CountryBalancesSummary.from(reportNode, initialNetPosition);
    }

    private static Country getCountry(final ReportNode reportNode) {
        final String name = getString(reportNode, REPORT_NODE_AREA_NAME_KEY);
        return stream(Country.values())
            .filter(country -> country.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown country name: %s".formatted(name)));
    }

}

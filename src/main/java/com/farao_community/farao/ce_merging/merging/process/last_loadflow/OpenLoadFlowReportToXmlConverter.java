/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.last_loadflow;

import com.farao_community.farao.ce_merging.xsd.execution_logs.Context;
import com.farao_community.farao.ce_merging.xsd.execution_logs.Logs;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UTC_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toStringNoTz;
import static java.time.LocalDateTime.now;

public final class OpenLoadFlowReportToXmlConverter {
    private static final String REPORT_SEVERITY = "reportSeverity";

    private OpenLoadFlowReportToXmlConverter() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static Logs fromOlfReportToXmlLogs(final ReportNode root) {
        final Logs logs = new Logs();

        if (root == null) {
            return logs;
        }

        final Context rootContext = createContext(root.getMessage());
        root.getChildren().forEach(child -> addRecordOrContext(child, rootContext, toStringNoTz(now(UTC_ZONE_ID))));

        logs.getCtxt().add(rootContext);

        return logs;
    }

    /**
     * Adds either a Record or a Context to the given parent Context,
     * according to the XSD rules and the ReportNode structure.
     */
    private static void addRecordOrContext(final ReportNode node,
                                           final Context parentContext,
                                           final String timestamp) {

        final Optional<TypedValue> severity = node.getValue(REPORT_SEVERITY);
        final Object toAdd;

        if (!node.getChildren().isEmpty() || severity.isEmpty()) {
            toAdd = toContext(node, timestamp);
        } else {
            toAdd = createRecord(timestamp, severity.get().toString(), node.getMessage());
        }

        parentContext.getRecOrCtxt().add(toAdd);
    }

    private static Context toContext(final ReportNode node,
                                     final String timestamp) {
        final Context context = createContext(node.getMessage());
        node.getChildren().forEach(child -> addRecordOrContext(child, context, timestamp));
        return context;
    }

    private static Context createContext(final String name) {
        final Context context = new Context();
        context.setNom(name);
        return context;
    }

    // fully qualified name because it would clash with java.lang.Record otherwise
    private static com.farao_community.farao.ce_merging.xsd.execution_logs.Record createRecord(final String timestamp,
                                                                                               final String level,
                                                                                               final String value) {
        final com.farao_community.farao.ce_merging.xsd.execution_logs.Record logRecord =
                new com.farao_community.farao.ce_merging.xsd.execution_logs.Record();
        logRecord.setDt(timestamp);
        logRecord.setLevel(level);
        logRecord.setValue(value);
        return logRecord;
    }

}

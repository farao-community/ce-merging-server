/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.final_result;

import com.farao_community.farao.ce_merging.xsd.execution_logs.Context;
import com.farao_community.farao.ce_merging.xsd.execution_logs.Logs;
import com.powsybl.commons.report.ReportNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Converts an OpenLoadFlow {@link ReportNode} tree into the JAXB XML Logs structure.
 *
 */
public final class OpenLoadFlowReportToXmlConverter {
    private static final String REPORT_SEVERITY = "reportSeverity";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private OpenLoadFlowReportToXmlConverter() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static Logs convert(ReportNode root) {
        Logs logs = new Logs();

        if (root == null) {
            return logs;
        }

        String timestamp = now();
        Context rootContext = createContext(root.getMessage());
        root.getChildren().forEach(child -> addRecOrContext(child, rootContext, timestamp));

        logs.getCtxt().add(rootContext);

        return logs;
    }

    /**
     * Adds either a Record or a Context to the given parent Context,
     * according to the XSD rules and the ReportNode structure.
     */
    private static void addRecOrContext(ReportNode node, Context parentContext, String timestamp) {
        if (node.getChildren().isEmpty()) {
            node.getValue(REPORT_SEVERITY).ifPresentOrElse(
                    s -> parentContext.getRecOrCtxt().add(createRecord(timestamp, s.getValue().toString(), node.getMessage())),
                    () -> parentContext.getRecOrCtxt().add(toContext(node, timestamp))
            );
        } else {
            parentContext.getRecOrCtxt().add(toContext(node, timestamp));
        }
    }

    private static Context toContext(ReportNode node, String timestamp) {
        Context context = createContext(node.getMessage());
        node.getChildren().forEach(child -> addRecOrContext(child, context, timestamp));
        return context;
    }

    private static Context createContext(String name) {
        Context context = new Context();
        context.setNom(name);
        return context;
    }

    private static com.farao_community.farao.ce_merging.xsd.execution_logs.Record createRecord(final String timestamp,
                                                                                               final String level,
                                                                                               final String value) {
        final com.farao_community.farao.ce_merging.xsd.execution_logs.Record record = new com.farao_community.farao.ce_merging.xsd.execution_logs.Record();
        record.setDt(timestamp);
        record.setLevel(level);
        record.setValue(value);
        return record;
    }

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}

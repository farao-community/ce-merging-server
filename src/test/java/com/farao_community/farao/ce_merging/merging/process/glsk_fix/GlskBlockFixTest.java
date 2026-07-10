/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QuantityType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.TimeIntervalType;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlskBlockFixTest {

    private static final String GSK_NAME = "FR";
    private static final String VALID_INTERVAL = "2026-01-01T00:00:00Z/2026-12-31T23:59:59Z";
    private static final String INVALID_INTERVAL = "2026-01-01T00:00:00Z/2026-01-01T01:00:00Z";
    private static final Instant TARGET_DATE = Instant.parse("2026-06-01T00:00:00Z");
    public static final String NODE_1 = "NODE1";

    @Test
    void shouldRemoveAutoBlockOutsideTargetDate() {
        final GSKSeriesType series = createSeries();
        series.getAutoGSKBlock().add(createAutoBlock(GSK_NAME, INVALID_INTERVAL));

        GlskBlockFix.validateAndRemoveInvalidGskBlocks(
                new HashMap<>(),
                new HashMap<>(),
                series,
                TARGET_DATE,
                List.of()
        );

        assertTrue(series.getAutoGSKBlock().isEmpty());
    }

    @Test
    void shouldRemoveManualBlockOutsideTargetDate() {
        final GSKSeriesType series = createSeries();
        series.getManualGSKBlock().add(createManualBlock(GSK_NAME, INVALID_INTERVAL));

        GlskBlockFix.validateAndRemoveInvalidGskBlocks(
                new HashMap<>(),
                new HashMap<>(),
                series,
                TARGET_DATE,
                List.of()
        );

        assertTrue(series.getManualGSKBlock().isEmpty());
    }

    @Test
    void shouldRemoveInvalidManualNodePresentInReport() {
        final GSKSeriesType series = createSeries();

        final ManualGSKBlockType block = createManualBlock(GSK_NAME, VALID_INTERVAL);
        block.getManualNodes().add(createManualNode(NODE_1, 10));

        series.getManualGSKBlock().add(block);

        GlskBlockFix.validateAndRemoveInvalidGskBlocks(
                new HashMap<>(),
                new HashMap<>(),
                series,
                TARGET_DATE,
                List.of(createReportNode(NODE_1))
        );

        assertTrue(series.getManualGSKBlock().isEmpty());
    }

    @Test
    void shouldKeepValidManualNode() {
        final GSKSeriesType series = createSeries();

        final ManualGSKBlockType block = createManualBlock(GSK_NAME, VALID_INTERVAL);
        block.getManualNodes().add(createManualNode(NODE_1, 20));

        series.getManualGSKBlock().add(block);

        GlskBlockFix.validateAndRemoveInvalidGskBlocks(
                new HashMap<>(),
                new HashMap<>(),
                series,
                TARGET_DATE,
                List.of()
        );

        assertEquals(1, series.getManualGSKBlock().size());
        assertEquals(1, block.getManualNodes().size());
    }

    private GSKSeriesType createSeries() {
        return new GSKSeriesType();
    }

    private ManualGSKBlockType createManualBlock(final String gskName,
                                                 final String interval) {
        final ManualGSKBlockType block = new ManualGSKBlockType();
        block.setGSKName(createStringValue(gskName));
        block.setTimeInterval(createTimeInterval(interval));
        return block;
    }

    private AutoGSKBlockType createAutoBlock(final String gskName,
                                             final String interval) {
        final AutoGSKBlockType block = new AutoGSKBlockType();
        block.setGSKName(createStringValue(gskName));
        block.setTimeInterval(createTimeInterval(interval));
        return block;
    }

    private ManualNodesType createManualNode(final String nodeName,
                                             final int factor) {
        final ManualNodesType node = new ManualNodesType();
        node.setNodeName(createStringValue(nodeName));
        node.setFactor(createDecimalValue(factor));
        return node;
    }

    private ReportNode createReportNode(final String nodeId) {
        final ReportNode reportNode = mock(ReportNode.class);
        final TypedValue typedValue = mock(TypedValue.class);

        when(reportNode.getValue(anyString())).thenReturn(Optional.of(typedValue));
        when(typedValue.toString()).thenReturn(nodeId);

        return reportNode;
    }

    private IdentificationType createStringValue(final String value) {
        final IdentificationType type = new IdentificationType();
        type.setV(value);
        return type;
    }

    private TimeIntervalType createTimeInterval(final String value) {
        final TimeIntervalType type = new TimeIntervalType();
        type.setV(value);
        return type;
    }

    private QuantityType createDecimalValue(final int value) {
        final QuantityType type = new QuantityType();
        type.setV(BigDecimal.valueOf(value));
        return type;
    }
}
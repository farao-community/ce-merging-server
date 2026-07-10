/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.xsd.glsk_fix.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_CODE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_CODE;
import static com.powsybl.glsk.commons.GlskReports.NODE_ID_KEY;

public final class GlskBlockFix {

    static void validateAndRemoveInvalidGskBlocks(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                                  final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                                  final GSKSeriesType glsk,
                                                  final Instant targetDate,
                                                  final List<ReportNode> reportNodeList) {
        checkAutoGskBlocks(glsk, targetDate, reportNodeList);
        checkManualBlocks(incorrectGlskBlockValue, correctGlskBlockValue, glsk, targetDate, reportNodeList);
    }

    private static void checkAutoGskBlocks(final GSKSeriesType gskSeriesType, final Instant targetDate, final List<ReportNode> reportNodeList) {
        gskSeriesType.getAutoGSKBlock()
                .removeIf(block -> shouldRemoveAutoBlock(block, targetDate, reportNodeList));
    }

    private static void checkManualBlocks(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                          final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                          final GSKSeriesType gskSeriesType,
                                          final Instant targetDate,
                                          final List<ReportNode> reportNodeList) {

        gskSeriesType.getManualGSKBlock()
                .removeIf(block -> processManualBlock(
                        incorrectGlskBlockValue,
                        correctGlskBlockValue,
                        block,
                        targetDate,
                        reportNodeList
                ));
    }

    private static boolean processManualBlock(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                              final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                              final ManualGSKBlockType block,
                                              final Instant targetDate,
                                              final List<ReportNode> reportNodeList) {
        if (isAlegroVirtualHub(block.getGSKName().getV())) {
            return true;
        }
        if (isTargetDateOutsideBlockInterval(targetDate, block.getTimeInterval())) return true;
        final List<ManualNodesType> invalidNodes = block.getManualNodes()
                .stream()
                .filter(node -> existsInReport(node, reportNodeList))
                .toList();

        block.getManualNodes().removeAll(invalidNodes);

        final String gskName = block.getGSKName().getV();

        storeFactorValueByNodes(incorrectGlskBlockValue, gskName, invalidNodes);
        storeFactorValueByNodes(correctGlskBlockValue, gskName, block.getManualNodes());

        if (block.getManualNodes().isEmpty()) {
            GlskBlockRedispatcher.storeValue(incorrectGlskBlockValue, gskName, null, 0);
            return true;
        }
        GlskBlockRedispatcher.redispatchFactorValue(incorrectGlskBlockValue, correctGlskBlockValue, block);
        return false;
    }

    private static boolean isTargetDateOutsideBlockInterval(final Instant targetDate, final TimeIntervalType timeInterval) {
        final Interval interval = getInterval(timeInterval);
        if (!interval.contains(targetDate)) {
            return true;
        }
        return false;
    }

    private static boolean existsInReport(final ManualNodesType node,
                                          final List<ReportNode> reportNodeList) {
        final String nodeId = node.getNodeName().getV();
        return reportNodeList.stream().anyMatch(reportNode -> extractValue(reportNode, NODE_ID_KEY).equals(nodeId));
    }

    private static boolean isAlegroVirtualHub(final String gskName) {
        return VIRTUAL_HUB_ALEGRO_BE_CODE.equals(gskName) || VIRTUAL_HUB_ALEGRO_DE_CODE.equals(gskName);
    }

    private static boolean shouldRemoveAutoBlock(final AutoGSKBlockType block,
                                                 final Instant targetDate,
                                                 final List<ReportNode> reportNodeList) {
        if (isAlegroVirtualHub(block.getGSKName().getV())) {
            return true;
        }
        if (isTargetDateOutsideBlockInterval(targetDate, block.getTimeInterval())) return true;
        removeInvalidAutoNodes(block, reportNodeList);
        return block.getAutoNodes().isEmpty();
    }

    private static void removeInvalidAutoNodes(final AutoGSKBlockType block,
                                               final List<ReportNode> reportNodeList) {
        final Set<String> reportNodeIds = reportNodeList.stream()
                .map(node -> extractValue(node, NODE_ID_KEY))
                .collect(Collectors.toSet());
        block.getAutoNodes().removeIf(node -> reportNodeIds.contains(node.getNodeName().getV()));
    }

    private static String extractValue(final ReportNode reportNode,
                                       final String key) {
        return reportNode.getValue(key)
                .map(TypedValue::toString)
                .orElseThrow(() ->
                        new IllegalArgumentException("Missing value for key: " + key)
                );
    }

    private static void storeFactorValueByNodes(final Map<String, List<GlskRedispatchingEntity>> map, final String gskName, final List<ManualNodesType> manualNodesTypeList) {
        manualNodesTypeList.forEach(manualNodesType -> {
            final String nodeName = manualNodesType.getNodeName().getV();
            final double factor = manualNodesType.getFactor().getV().doubleValue();
            GlskBlockRedispatcher.storeValue(map, gskName, nodeName, factor);
        });
    }

    private static Interval getInterval(final TimeIntervalType timeInterval) {
        return Interval.parse(timeInterval.getV());
    }

}

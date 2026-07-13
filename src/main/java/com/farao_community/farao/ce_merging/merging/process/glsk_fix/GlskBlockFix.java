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
import java.util.ArrayList;
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
        gskSeriesType.getAutoGSKBlock().removeIf(block -> {
            if (isAlegroHub(block.getGSKName().getV()) || !getInterval(block.getTimeInterval()).contains(targetDate)) {
                return true;
            }
            checkAutoGskNodes(block, reportNodeList);
            return block.getAutoNodes().isEmpty();
        });
    }

    private static void checkAutoGskNodes(final AutoGSKBlockType autoGSKBlockType, final List<ReportNode> reportNodeList) {
        final Set<String> reportNodeIds = reportNodeList.stream()
                .map(reportNode -> extractValue(reportNode, NODE_ID_KEY))
                .collect(Collectors.toSet());

        autoGSKBlockType.getAutoNodes()
                .removeIf(autoNode ->
                        reportNodeIds.contains(autoNode.getNodeName().getV()));
    }

    private static void checkManualBlocks(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                          final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                          final GSKSeriesType gskSeriesType,
                                          final Instant targetDate,
                                          final List<ReportNode> reportNodeList) {
        final List<ManualGSKBlockType> manualNodesBlocks = new ArrayList<>();

        gskSeriesType.getManualGSKBlock().forEach(manualGSKBlockType -> {
            if (isAlegroHub(manualGSKBlockType.getGSKName().getV())) {
                manualNodesBlocks.add(manualGSKBlockType);
            }
            final Interval timeInterval = getInterval(manualGSKBlockType.getTimeInterval());
            if (timeInterval.contains(targetDate)) {
                checkManualNodes(incorrectGlskBlockValue, correctGlskBlockValue, manualGSKBlockType, reportNodeList);
            } else {
                manualNodesBlocks.add(manualGSKBlockType);
            }
            if (manualGSKBlockType.getManualNodes().isEmpty()) {
                storeFactorValueByNodes(incorrectGlskBlockValue, manualGSKBlockType.getGSKName().getV(), manualGSKBlockType.getManualNodes());
                manualNodesBlocks.add(manualGSKBlockType);
            }

            GlskBlockRedispatcher.redispatchFactorValue(incorrectGlskBlockValue, correctGlskBlockValue, manualGSKBlockType);

        });

        gskSeriesType.getManualGSKBlock().removeAll(manualNodesBlocks);
    }

    private static void checkManualNodes(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                         final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                         final ManualGSKBlockType manualGSKBlockType,
                                         final List<ReportNode> reportNodeList) {
        final List<ManualNodesType> nodesToRemove = new ArrayList<>();

        manualGSKBlockType.getManualNodes().forEach(manualNodesType -> {
            reportNodeList.stream()
                    .filter(reportNode -> extractValue(reportNode, NODE_ID_KEY).equals(manualNodesType.getNodeName().getV()))
                    .map(reportNode -> manualNodesType)
                    .forEach(nodesToRemove::add);
        });

        manualGSKBlockType.getManualNodes().removeAll(nodesToRemove);

        final String gskName = manualGSKBlockType.getGSKName().getV();
        final List<ManualNodesType> manualNodesTypeList = manualGSKBlockType.getManualNodes();

        storeFactorValueByNodes(incorrectGlskBlockValue, gskName, nodesToRemove);
        storeFactorValueByNodes(correctGlskBlockValue, gskName, manualNodesTypeList);
    }

    private static void storeFactorValueByNodes(final Map<String, List<GlskRedispatchingEntity>> map, final String gskName, final List<ManualNodesType> manualNodesTypeList) {
        manualNodesTypeList.forEach(manualNodesType -> {
            final String nodeName = manualNodesType.getNodeName().getV();
            final double factor = manualNodesType.getFactor().getV().doubleValue();
            GlskBlockRedispatcher.storeValue(map, gskName, nodeName, factor);
        });
    }

    private static boolean isAlegroHub(final String gskName) {
        return VIRTUAL_HUB_ALEGRO_BE_CODE.equals(gskName) || VIRTUAL_HUB_ALEGRO_DE_CODE.equals(gskName);
    }

    private static String extractValue(final ReportNode reportNode, final String key) {
        return reportNode.getValue(key).map(TypedValue::toString).orElseThrow(() -> new IllegalArgumentException("Missing value for key: " + key));
    }

    private static Interval getInterval(final TimeIntervalType timeInterval) {
        return Interval.parse(timeInterval.getV());
    }

}

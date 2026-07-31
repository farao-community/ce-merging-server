/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.TimeIntervalType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
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

    static void removeInvalidGskBlocks(final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                       final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                       final GSKSeriesType glsk,
                                       final Instant targetDate,
                                       final List<ReportNode> reportNodeList) {
        filterAutoGskBlocks(glsk.getAutoGSKBlock(), targetDate, reportNodeList);
        processManualBlocks(glsk.getManualGSKBlock(), incorrectValuesByGskName, correctValuesByGskName, targetDate, reportNodeList);
    }

    private static void filterAutoGskBlocks(final List<AutoGSKBlockType> blocks, final Instant targetDate, final List<ReportNode> reportNodeList) {
        blocks.removeIf(block -> {
            if (isAlegroHub(block.getGSKName()) || !isInTimeInterval(block.getTimeInterval(), targetDate)) {
                return true;
            }
            removeInvalidAutoGskNodes(block, reportNodeList);
            return block.getAutoNodes().isEmpty();
        });
    }

    private static void removeInvalidAutoGskNodes(final AutoGSKBlockType autoGSKBlock, final List<ReportNode> reportNodeList) {
        final Set<String> reportNodeIds = reportNodeList.stream()
                .map(reportNode -> extractValue(reportNode, NODE_ID_KEY))
                .collect(Collectors.toSet());

        autoGSKBlock.getAutoNodes()
                .removeIf(autoNode -> reportNodeIds.contains(autoNode.getNodeName().getV()));
    }

    private static void processManualBlocks(final List<ManualGSKBlockType> blocks,
                                            final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                            final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                            final Instant targetDate,
                                            final List<ReportNode> reportNodeList) {
        final List<ManualGSKBlockType> manualNodesBlocks = new ArrayList<>();

        blocks.forEach(manualGskBlock -> {
            if (isAlegroHub(manualGskBlock.getGSKName())) {
                manualNodesBlocks.add(manualGskBlock);
            }
            if (isInTimeInterval(manualGskBlock.getTimeInterval(), targetDate)) {
                removeInvalidManualNodesAndStoreFactors(incorrectValuesByGskName, correctValuesByGskName, manualGskBlock, reportNodeList);
            } else {
                manualNodesBlocks.add(manualGskBlock);
            }
            if (manualGskBlock.getManualNodes().isEmpty()) {
                storeFactorValueByNodes(incorrectValuesByGskName, manualGskBlock.getGSKName().getV(), manualGskBlock.getManualNodes());
                manualNodesBlocks.add(manualGskBlock);
            }

            GlskBlockRedispatcher.redispatchFactorValue(incorrectValuesByGskName, correctValuesByGskName, manualGskBlock);

        });

        blocks.removeAll(manualNodesBlocks);
    }

    private static void removeInvalidManualNodesAndStoreFactors(final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                                                final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                                                final ManualGSKBlockType manualGSKBlock,
                                                                final List<ReportNode> reportNodeList) {
        final List<ManualNodesType> nodesToRemove = new ArrayList<>();

        manualGSKBlock.getManualNodes().forEach(manualNodes -> {
            reportNodeList.stream()
                    .filter(reportNode -> extractValue(reportNode, NODE_ID_KEY).equals(manualNodes.getNodeName().getV()))
                    .map(reportNode -> manualNodes)
                    .forEach(nodesToRemove::add);
        });

        manualGSKBlock.getManualNodes().removeAll(nodesToRemove);

        final String gskName = manualGSKBlock.getGSKName().getV();
        final List<ManualNodesType> manualNodesList = manualGSKBlock.getManualNodes();

        storeFactorValueByNodes(incorrectValuesByGskName, gskName, nodesToRemove);
        storeFactorValueByNodes(correctValuesByGskName, gskName, manualNodesList);
    }

    private static void storeFactorValueByNodes(final Map<String, List<GlskRedispatchingEntity>> valuesByGskName, final String gskName, final List<ManualNodesType> manualNodesList) {
        manualNodesList.forEach(manualNode -> {
            final String nodeName = manualNode.getNodeName().getV();
            final double factor = manualNode.getFactor().getV().doubleValue();
            GlskBlockRedispatcher.storeValue(valuesByGskName, gskName, nodeName, factor);
        });
    }

    private static boolean isAlegroHub(final IdentificationType gskName) {
        return VIRTUAL_HUB_ALEGRO_BE_CODE.equals(gskName.getV()) || VIRTUAL_HUB_ALEGRO_DE_CODE.equals(gskName.getV());
    }

    private static String extractValue(final ReportNode reportNode, final String key) {
        return reportNode.getValue(key).map(TypedValue::toString).orElseThrow(() -> new IllegalArgumentException("Missing value for key: " + key));
    }

    private static boolean isInTimeInterval(final TimeIntervalType timeInterval, final Instant targetDate) {
        return Interval.parse(timeInterval.getV()).contains(targetDate);
    }

}

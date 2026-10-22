/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskSerieShare;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.TimeIntervalType;
import com.powsybl.commons.report.ReportNode;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskQualityCheckService.NODE_ID_KEY;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.addShare;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.getFactor;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.getName;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.getReportValue;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.hasEntry;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.isAlegroBlock;
import static java.util.stream.Collectors.toSet;

public final class GlskBlockFix {

    static void removeInvalidGskBlocks(final Map<String, List<GlskSerieShare>> incorrectValuesByGskName,
                                       final Map<String, List<GlskSerieShare>> correctValuesByGskName,
                                       final GSKSeriesType glsk,
                                       final Instant targetDate,
                                       final List<ReportNode> reportNodeList) {
        glsk.getAutoGSKBlock().removeIf(block -> isInvalid(block, targetDate, reportNodeList));
        processManualBlocks(glsk.getManualGSKBlock(), incorrectValuesByGskName, correctValuesByGskName, targetDate, reportNodeList);
    }

    private static boolean isInvalid(final AutoGSKBlockType block,
                                     final Instant targetDate,
                                     final List<ReportNode> reportNodeList) {
        if (isAlegroBlock(block) || !isInTimeInterval(targetDate, block.getTimeInterval())) {
            return true;
        }
        removeInvalidAutoGskNodes(block, reportNodeList);
        return block.getAutoNodes().isEmpty();
    }

    private static void removeInvalidAutoGskNodes(final AutoGSKBlockType autoGSKBlock,
                                                  final List<ReportNode> reportNodes) {
        final Set<String> reportNodeIds = reportNodes.stream()
                .map(report -> getReportValue(report, NODE_ID_KEY))
                .collect(toSet());

        autoGSKBlock.getAutoNodes()
                .removeIf(autoNode -> reportNodeIds.contains(getName(autoNode)));
    }

    private static void processManualBlocks(final List<ManualGSKBlockType> blocks,
                                            final Map<String, List<GlskSerieShare>> incorrectValuesByGskName,
                                            final Map<String, List<GlskSerieShare>> correctValuesByGskName,
                                            final Instant targetDate,
                                            final List<ReportNode> reportNodes) {
        final List<ManualGSKBlockType> invalidManualBlocks = blocks.stream()
                .map(block -> processManualBlock(block, incorrectValuesByGskName, correctValuesByGskName, targetDate, reportNodes))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        blocks.removeAll(invalidManualBlocks);
    }

    private static Optional<ManualGSKBlockType> processManualBlock(final ManualGSKBlockType block,
                                                                   final Map<String, List<GlskSerieShare>> incorrectValuesByGskName,
                                                                   final Map<String, List<GlskSerieShare>> correctValuesByGskName,
                                                                   final Instant targetDate,
                                                                   final List<ReportNode> reportNodes) {
        boolean isValidBlock = !isAlegroBlock(block);

        if (isInTimeInterval(targetDate, block.getTimeInterval())) {
            removeInvalidManualNodesAndStoreFactors(incorrectValuesByGskName, correctValuesByGskName, block, reportNodes);
        } else {
            isValidBlock = false;
        }

        final List<ManualNodesType> manualNodes = block.getManualNodes();

        if (manualNodes.isEmpty()) {
            storeFactorsByNodes(incorrectValuesByGskName, block.getGSKName().getV(), manualNodes);
            isValidBlock = false;
        }

        GlskBlockRedispatcher.redispatchFactorValue(incorrectValuesByGskName, correctValuesByGskName, block);

        return isValidBlock ? Optional.empty() : Optional.of(block);
    }

    private static void removeInvalidManualNodesAndStoreFactors(final Map<String, List<GlskSerieShare>> incorrectGskByName,
                                                                final Map<String, List<GlskSerieShare>> correctGskByName,
                                                                final ManualGSKBlockType block,
                                                                final List<ReportNode> reportNodes) {
        final List<ManualNodesType> manualNodes = block.getManualNodes();
        final List<ManualNodesType> nodesToRemove = manualNodes.stream()
                .filter(isInReports(reportNodes))
                .toList();

        manualNodes.removeAll(nodesToRemove);

        final String gskName = block.getGSKName().getV();

        storeFactorsByNodes(incorrectGskByName, gskName, nodesToRemove);
        storeFactorsByNodes(correctGskByName, gskName, manualNodes);
    }

    private static Predicate<ManualNodesType> isInReports(final List<ReportNode> reportNodes) {
        return node -> reportNodes.stream().anyMatch(hasEntry(NODE_ID_KEY, getName(node)));
    }

    private static void storeFactorsByNodes(final Map<String, List<GlskSerieShare>> valuesByGskName,
                                            final String gskName,
                                            final List<ManualNodesType> manualNodes) {
        manualNodes.forEach(node -> addShare(valuesByGskName, gskName, getName(node), getFactor(node)));
    }

    private static boolean isInTimeInterval(final Instant targetDate,
                                            final TimeIntervalType timeInterval) {
        return Interval.parse(timeInterval.getV()).contains(targetDate);
    }

}

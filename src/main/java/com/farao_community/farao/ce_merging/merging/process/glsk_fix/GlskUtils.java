/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskSerieShare;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isAlegroNode;

public final class GlskUtils {

    private GlskUtils() {
        // utilities
    }

    public static String getName(final ManualNodesType node) {
        return node.getNodeName().getV();
    }

    public static String getName(final AutoNodesType node) {
        return node.getNodeName().getV();
    }

    public static boolean isAlegroBlock(final ManualGSKBlockType block) {
        return isAlegroNode(block.getGSKName().getV());
    }

    public static boolean isAlegroBlock(final AutoGSKBlockType block) {
        return isAlegroNode(block.getGSKName().getV());
    }

    public static String getReportValue(final ReportNode reportNode,
                                        final String key) {
        return reportNode.getValue(key)
                .map(TypedValue::toString)
                .orElseThrow(() -> new IllegalArgumentException("Missing report value: " + key));
    }

    public static Predicate<ReportNode> hasEntry(final String key,
                                                 final String value) {
        return report -> report.getValue(key).map(Object::toString).filter(value::equals).isPresent();
    }

    public static double getFactor(final ManualNodesType node) {
        return node.getFactor().getV().doubleValue();
    }

    public static void setFactor(final ManualNodesType node,
                                 final double factor) {
        node.getFactor().setV(BigDecimal.valueOf(factor));
    }

    public static void addShare(final Map<String, List<GlskSerieShare>> sharesByKey,
                                final String key,
                                final String shareId,
                                final double shareValue) {
        sharesByKey.computeIfAbsent(key, s -> new ArrayList<>())
                .add(new GlskSerieShare(shareId, shareValue));
    }
}

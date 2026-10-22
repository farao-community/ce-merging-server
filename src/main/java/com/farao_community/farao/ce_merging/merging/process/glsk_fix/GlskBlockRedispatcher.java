/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskSerieShare;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumProperty;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.getFactor;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.setFactor;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingDouble;

public final class GlskBlockRedispatcher {
    private static final double ROUNDING_SCALE = 1e6;
    private static final double EXPECTED_FACTOR_SUM = 1.0;

    private GlskBlockRedispatcher() {
    }

    public static void redispatchFactorValue(final Map<String, List<GlskSerieShare>> incorrectValuesByGskName,
                                             final Map<String, List<GlskSerieShare>> correctValuesByGskName,
                                             final ManualGSKBlockType manualGSKBlock) {
        final String gskName = Optional.ofNullable(manualGSKBlock.getGSKName())
                .map(IdentificationType::getV)
                .orElseThrow(() -> new CeMergingException("Missing GLSK block name"));
        final List<ManualNodesType> nodes = Optional.ofNullable(manualGSKBlock.getManualNodes())
                .orElseThrow(() -> new CeMergingException("Missing manual nodes for GLSK block " + gskName));

        if (incorrectValuesByGskName.containsKey(gskName) && correctValuesByGskName.containsKey(gskName)) {
            nodes.forEach(node -> {
                final double factor = getRedispatchFactor(incorrectValuesByGskName, correctValuesByGskName,
                                                          gskName, tryToGetFactor(node));
                setFactor(node, factor);
            });
        }

        final double factorSum = sumProperty(nodes, GlskBlockRedispatcher::tryToGetFactor);

        if (factorSum != EXPECTED_FACTOR_SUM) {
            normalizeFactors(factorSum, nodes);
        }
    }

    private static double tryToGetFactor(final ManualNodesType node) {
        if (node.getFactor() == null || node.getFactor().getV() == null) {
            throw new CeMergingException("Missing factor value for GLSK node");
        }
        return getFactor(node);
    }

    private static double getRedispatchFactor(final Map<String, List<GlskSerieShare>> incorrectValuesByGskName,
                                              final Map<String, List<GlskSerieShare>> correctValuesByGskName,
                                              final String gskName,
                                              double factorValue) {
        final double correctSum = calculateBlockSum(correctValuesByGskName, gskName);
        final double incorrectSum = calculateBlockSum(incorrectValuesByGskName, gskName);

        if (correctSum == 0.0) {
            throw new CeMergingException(String.format("Cannot redispatch GLSK block '%s': correct block sum is zero", gskName));
        }
        return roundGlskFactor(factorValue + incorrectSum * factorValue / correctSum);
    }

    private static double calculateBlockSum(final Map<String, List<GlskSerieShare>> valueByGskName,
                                            final String gskName) {

        double sum = 0.;
        final Set<String> seenIds = new HashSet<>();
        final List<GlskSerieShare> shares = valueByGskName.getOrDefault(gskName, emptyList());

        // as we want the last occurrences of ids, we iterate backwards
        for (int i = shares.size() - 1; i > 0; i--) {
            final GlskSerieShare share = shares.get(i);
            if (seenIds.add(share.id())) {
                sum += share.share();
            }
        }

        return sum;
    }

    private static void normalizeFactors(final double factorSum,
                                         final List<ManualNodesType> nodes) {
        final double gapToNormal = EXPECTED_FACTOR_SUM - factorSum;
        nodes.stream()
                .max(comparingDouble(GlskUtils::getFactor))
                .filter(node -> getFactor(node) > 0)
                .ifPresent(maxNode -> setFactor(maxNode, getFactor(maxNode) + gapToNormal));
    }

    private static double roundGlskFactor(final double factor) {
        return Math.round(factor * ROUNDING_SCALE) / ROUNDING_SCALE;
    }
}

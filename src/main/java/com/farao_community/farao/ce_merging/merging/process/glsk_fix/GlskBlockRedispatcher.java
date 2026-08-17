/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GlskBlockRedispatcher {
    private static final double ROUNDING_SCALE = 1e6;
    private static final double EXPECTED_FACTOR_SUM = 1.0;

    private GlskBlockRedispatcher() {
    }

    static void storeValue(final Map<String, List<GlskRedispatchingEntity>> redispatchingEntitiesByGskName, final String gskName, final String nodeName, final double factor) {
        redispatchingEntitiesByGskName.computeIfAbsent(gskName, key -> new ArrayList<>()).add(new GlskRedispatchingEntity(nodeName, factor));
    }

    static void redispatchFactorValue(final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                      final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                      final ManualGSKBlockType manualGSKBlock) {
        final String gskName = Optional.ofNullable(manualGSKBlock.getGSKName())
                .map(IdentificationType::getV)
                .orElseThrow(() -> new CeMergingException("Missing GLSK block name"));
        final List<ManualNodesType> nodes = Optional.ofNullable(manualGSKBlock.getManualNodes())
                .orElseThrow(() -> new CeMergingException("Missing manual nodes for GLSK block " + gskName));

        nodes.forEach(node -> fixFactorValue(incorrectValuesByGskName, correctValuesByGskName, gskName, node));

        final double factorSum = nodes.stream()
                .mapToDouble(node -> {
                    if (node.getFactor() == null || node.getFactor().getV() == null) {
                        throw new CeMergingException("Missing factor value for GLSK node");
                    }
                    return node.getFactor().getV().doubleValue();
                })
                .sum();
        if (factorSum != EXPECTED_FACTOR_SUM) {
            normalizeFactors(factorSum, nodes);
        }
    }

    private static void fixFactorValue(final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                       final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                       final String gskName,
                                       final ManualNodesType manualNode) {
        if (incorrectValuesByGskName.containsKey(gskName) && correctValuesByGskName.containsKey(gskName)) {
            if (manualNode.getFactor() == null || manualNode.getFactor().getV() == null) {
                throw new CeMergingException("Missing factor value for GLSK node");
            }
            final double factorValue = manualNode.getFactor().getV().doubleValue();
            final double newFactor = calculateRedispatchedFactorValue(incorrectValuesByGskName, correctValuesByGskName, gskName, factorValue);
            manualNode.getFactor().setV(BigDecimal.valueOf(newFactor));
        }
    }

    private static double calculateRedispatchedFactorValue(final Map<String, List<GlskRedispatchingEntity>> incorrectValuesByGskName,
                                                           final Map<String, List<GlskRedispatchingEntity>> correctValuesByGskName,
                                                           final String gskName,
                                                           double factorValue) {
        final double correctSum = calculateBlockSum(correctValuesByGskName, gskName);
        final double incorrectSum = calculateBlockSum(incorrectValuesByGskName, gskName);
        if (correctSum == 0.0) {
            throw new CeMergingException(String.format("Cannot redispatch GLSK block '%s': correct block sum is zero", gskName));
        }
        return roundGlsk(factorValue + incorrectSum * factorValue / correctSum);
    }

    private static double calculateBlockSum(final Map<String, List<GlskRedispatchingEntity>> valueByGskName, final String gskName) {
        return valueByGskName.getOrDefault(gskName, Collections.emptyList()).stream()
                .collect(Collectors.toMap(
                        GlskRedispatchingEntity::getId,
                        GlskRedispatchingEntity::getShare,
                        (existing, replacement) -> replacement
                ))
                .values()
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    private static void normalizeFactors(final double factorSum, final List<ManualNodesType> nodes) {
        final double difference = factorSum - EXPECTED_FACTOR_SUM;
        nodes.stream()
                .max(Comparator.comparingDouble((ManualNodesType node) -> getFactor(node)))
                .filter(node -> getFactor(node) > 0)
                .ifPresent(maxNode -> {
                    final double factorValue = getFactor(maxNode);
                    maxNode.getFactor().setV(BigDecimal.valueOf(factorValue - difference));
                });
    }

    private static double getFactor(final ManualNodesType node) {
        return node.getFactor().getV().doubleValue();
    }

    private static double roundGlsk(double factorValueToRound) {
        return Math.round(factorValueToRound * ROUNDING_SCALE) / ROUNDING_SCALE;
    }
}

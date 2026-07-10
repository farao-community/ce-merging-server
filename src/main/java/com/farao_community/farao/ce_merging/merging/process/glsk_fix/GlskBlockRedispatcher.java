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

    static void storeValue(final Map<String, List<GlskRedispatchingEntity>> map, final String gskName, String nodeName, final double factor) {
        map.computeIfAbsent(gskName, s -> new ArrayList<>()).add(new GlskRedispatchingEntity(nodeName, factor));
    }

    static void redispatchFactorValue(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                      final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                      final ManualGSKBlockType manualGSKBlockType) {
        final String gskName = Optional.ofNullable(manualGSKBlockType.getGSKName())
                .map(IdentificationType::getV)
                .orElseThrow(() -> new CeMergingException("Missing GLSK block name"));
        final List<ManualNodesType> nodes = Optional.ofNullable(manualGSKBlockType.getManualNodes())
                .orElseThrow(() -> new CeMergingException("Missing manual nodes for GLSK block " + gskName));

        nodes.forEach(node -> fixFactorValue(incorrectGlskBlockValue, correctGlskBlockValue, gskName, node));

        final double factorSum = nodes.stream()
                .mapToDouble(node -> Optional.ofNullable(node.getFactor())
                        .map(factor -> factor.getV())
                        .map(BigDecimal::doubleValue)
                        .orElseThrow(() -> new CeMergingException("Missing factor value for GLSK node")))
                .sum();
        if (Double.compare(factorSum, EXPECTED_FACTOR_SUM) != 0) {
            normalizeFactors(factorSum, nodes);
        }
    }

    private static void fixFactorValue(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                       final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                       final String gskName,
                                       final ManualNodesType manualNodesType) {
        if (incorrectGlskBlockValue.containsKey(gskName) && correctGlskBlockValue.containsKey(gskName)) {
            double factorValue = Optional.ofNullable(manualNodesType.getFactor())
                    .map(factor -> factor.getV())
                    .map(BigDecimal::doubleValue).orElseThrow(() -> new CeMergingException("Missing factor value for GLSK node"));
            double newFactor = fixValueCalculation(incorrectGlskBlockValue, correctGlskBlockValue, gskName, factorValue);
            manualNodesType.getFactor().setV(BigDecimal.valueOf(newFactor));
        }
    }

    private static double fixValueCalculation(final Map<String, List<GlskRedispatchingEntity>> incorrectGlskBlockValue,
                                              final Map<String, List<GlskRedispatchingEntity>> correctGlskBlockValue,
                                              final String gskName,
                                              double factorValue) {
        final double correctSum = calculateBlockSum(correctGlskBlockValue, gskName);
        final double incorrectSum = calculateBlockSum(incorrectGlskBlockValue, gskName);
        if (Double.compare(correctSum, 0.0) == 0) {
            throw new CeMergingException(String.format("Cannot redispatch GLSK block '%s': correct block sum is zero", gskName));
        }
        return roundGlsk(factorValue + incorrectSum * factorValue / correctSum);
    }

    private static double calculateBlockSum(final Map<String, List<GlskRedispatchingEntity>> glskBlockValue, final String gskName) {
        return glskBlockValue.getOrDefault(gskName, Collections.emptyList()).stream()
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
                .max(Comparator.comparingDouble(node -> node.getFactor().getV().doubleValue()))
                .filter(node -> node.getFactor().getV().doubleValue() > 0)
                .ifPresent(maxNode -> {
                    maxNode.getFactor().setV(BigDecimal.valueOf(maxNode.getFactor().getV().doubleValue() - difference));
                });
    }

    private static double roundGlsk(double v) {
        return Math.round(v * ROUNDING_SCALE) / ROUNDING_SCALE;
    }
}

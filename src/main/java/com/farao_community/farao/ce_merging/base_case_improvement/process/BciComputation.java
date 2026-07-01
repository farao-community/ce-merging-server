/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.Interval;
import com.farao_community.farao.ce_merging.base_case_improvement.forecast_netpositions.ReferenceProgram;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciAreaResults;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.BciComputationResult;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.GlobalNetPositions;
import com.farao_community.farao.ce_merging.base_case_improvement.process.result.InRegionNetPositions;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciComputation {
    private static final double EPSILON = 0.01;
    private static final Logger LOGGER = LoggerFactory.getLogger(BciComputation.class);

    private final RegionConfiguration region;
    private final ReferenceProgram referenceProgram;
    private Map<String, Double> inRegionNpfByArea = new HashMap<>();
    private Map<String, Double> targetInRegionNpByArea = new HashMap<>();
    private Map<String, Double> globalNpfByArea = new HashMap<>();
    private Map<String, Double> targetGlobalNpByArea = new HashMap<>();
    private final Map<String, Boolean> bciAppliedByArea = new HashMap<>();

    BciComputation(final RegionConfiguration region, final ReferenceProgram referenceProgram) {
        this.region = region;
        this.referenceProgram = referenceProgram;
    }

    public BciComputationResult run(final Map<String, Interval> regionFeasibilityRanges,
                                    final Map<String, Double> initialRegionNetPositions,
                                    final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        final Map<String, BciAreaResults> results;

        if (!validFeasibilityRanges(regionFeasibilityRanges)) {
            LOGGER.error("The feasibility ranges are not valid");
            throw new CeMergingException("The feasibility ranges are not valid");
        }

        // compute forecast net position inside region and exchange out region for each area
        inRegionNpfByArea = referenceProgram.computeAllNetPositionsInRegion(region);
        globalNpfByArea = referenceProgram.computeAllGlobalNetPositions(region);
        shiftNpfWithAlegro(inRegionNpfByArea, globalNpfByArea, alBeToCeFlow, alDeToCeFlow);

        final Map<String, Double> outNetPositionByArea = referenceProgram.computeAllNetPositionsOutRegion(region);

        // check net position in feasibility ranges
        if (isNpfValid(inRegionNpfByArea, regionFeasibilityRanges)) {
            LOGGER.info("All forecast net positions are in the feasibility ranges, Bci is not applied");
            targetInRegionNpByArea = inRegionNpfByArea;
            targetGlobalNpByArea = globalNpfByArea;
            results = createResults(regionFeasibilityRanges, initialRegionNetPositions);
            results.forEach((key, value) -> value.setBciApplied(FALSE));
            return new BciComputationResult(false, false, results);
        } else {
            LOGGER.info("Not all forecast net positions are in the feasibility ranges, Bci will be applied");
            final Pair<Boolean, Map<String, Double>> resultBci = applyBci(inRegionNpfByArea, regionFeasibilityRanges, bciAppliedByArea);
            targetInRegionNpByArea = resultBci.getRight();
            targetGlobalNpByArea = targetInRegionNpByArea.entrySet().stream()
                .collect(toMap(Entry::getKey,
                               e -> e.getValue() + outNetPositionByArea.get(e.getKey())));
            results = createResults(regionFeasibilityRanges, initialRegionNetPositions);
            return new BciComputationResult(true, resultBci.getLeft(), results);
        }
    }

    private void shiftNpfWithAlegro(final Map<String, Double> inRegionNpfByArea,
                                    final Map<String, Double> globalNpfByArea,
                                    final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        // Bci process should not take Alegro flows into account.
        // ForecastNetPositionInRegionByArea = BE-CE from NPF file which contains already ALBE-CE flow
        // That's why we must subtract from it ALBE-CE flow to have only the AC target flow as the BCI target flow.
        final String belgium = region.getAreasIn().get("BE");
        final String germany = region.getAreasIn().get("DE");

        globalNpfByArea.computeIfPresent(belgium, (k, np) -> np + alBeToCeFlow);
        inRegionNpfByArea.computeIfPresent(belgium, (k, np) -> np + alBeToCeFlow);
        globalNpfByArea.computeIfPresent(germany, (k, np) -> np + alDeToCeFlow);
        inRegionNpfByArea.computeIfPresent(germany, (k, np) -> np + alDeToCeFlow);
    }

    private Map<String, BciAreaResults> createResults(final Map<String, Interval> regionFeasibilityRanges,
                                                      final Map<String, Double> initialInRegionNpByArea) {
        return region.getAreasIn().entrySet().stream()
            .collect(toMap(Entry::getKey, entry -> this.createBciAreaResult(
                entry.getValue(), regionFeasibilityRanges, initialInRegionNpByArea, bciAppliedByArea))
            );
    }

    private BciAreaResults createBciAreaResult(final String areaId,
                                               final Map<String, Interval> regionFeasibilityRanges,
                                               final Map<String, Double> initialRegionNpByArea,
                                               final Map<String, Boolean> bciAppliedByArea) {
        final double targetRegionNp = targetInRegionNpByArea.get(areaId);
        final double regionMinFeasible = regionFeasibilityRanges.get(areaId).getMinValue();
        final double regionMaxFeasible = regionFeasibilityRanges.get(areaId).getMaxValue();

        final InRegionNetPositions inRegionResults = new InRegionNetPositions(
            initialRegionNpByArea.getOrDefault(areaId, 0.),
            regionMinFeasible,
            regionMaxFeasible,
            min(regionMinFeasible, targetRegionNp),
            max(regionMaxFeasible, targetRegionNp),
            inRegionNpfByArea.get(areaId),
            targetRegionNp
        );

        final Boolean isBciApplied = bciAppliedByArea.get(areaId);
        final GlobalNetPositions globalResults = new GlobalNetPositions(globalNpfByArea.get(areaId),
                                                                        targetGlobalNpByArea.get(areaId));

        return new BciAreaResults(inRegionResults, globalResults, isBciApplied);
    }

    private boolean validFeasibilityRanges(final Map<String, Interval> regionFeasibilityRanges) {
        return regionFeasibilityRanges.keySet().containsAll(region.getAreasIn().values());
    }

    private static boolean isNpfValid(final Map<String, Double> inRegionNpByArea,
                                      final Map<String, Interval> regionFeasibilityRanges) {

        return inRegionNpByArea.entrySet().stream()
            .allMatch((e -> regionFeasibilityRanges.get(e.getKey()).containsValue(e.getValue())));
    }

    private static boolean isNotNegligible(final double value) {
        return BigDecimal.valueOf(value).abs().compareTo(BigDecimal.valueOf(EPSILON)) > 0;
    }

    private static Pair<Boolean, Map<String, Double>> applyBci(final Map<String, Double> refNetPositionInRegionByArea,
                                                               final Map<String, Interval> regionFeasibilityRanges,
                                                               final Map<String, Boolean> bciAppliedByArea) {
        Map<String, Double> targetInRegionNpByArea = new HashMap<>(refNetPositionInRegionByArea);
        targetInRegionNpByArea.keySet().forEach(areaId -> bciAppliedByArea.put(areaId, FALSE));

        // Initial violation

        Map<String, Double> violationsByArea = computeViolationsByArea(targetInRegionNpByArea, regionFeasibilityRanges);
        double totalViolations = sumOfValues(violationsByArea);
        final double contraryViolations = computeContraryViolations(violationsByArea, totalViolations);

        // Step 1, Preliminary BCI to solve contrary violations

        if (isNotNegligible(contraryViolations)) {
            LOGGER.info("Total violation {}, Contrary violations : {} , Prelimanry BC to solve contrary violations will be aplied", totalViolations, contraryViolations);
            applyBciStep1(targetInRegionNpByArea, regionFeasibilityRanges, violationsByArea, bciAppliedByArea, totalViolations, contraryViolations);
            violationsByArea = computeViolationsByArea(targetInRegionNpByArea, regionFeasibilityRanges);
            totalViolations = sumOfValues(violationsByArea);
        }

        // Step 2, solving of violations while respecting feasibility ranges

        if (isNotNegligible(totalViolations)) {
            LOGGER.info("Total violations after solving contrary  violation : {}, Normal BCI will be applied", totalViolations);
            applyBciStep2(targetInRegionNpByArea, regionFeasibilityRanges, violationsByArea, bciAppliedByArea, totalViolations);
        }

        // Step 3, Compensation of remaining imbalance

        double totalImbalance = sumOfValues(targetInRegionNpByArea);

        boolean isExtendedBciFeasibilityRanges = false;

        if (isNotNegligible(totalImbalance)) {
            LOGGER.info("Total imbalance after BCI : {}, compensation of remaining imbalance will be applied", totalImbalance);
            applyBciStep3(targetInRegionNpByArea, regionFeasibilityRanges, totalImbalance);
            isExtendedBciFeasibilityRanges = true;
        }

        return Pair.of(isExtendedBciFeasibilityRanges, targetInRegionNpByArea);
    }

    private static double sumOfValues(final Map<String, Double> valuesByArea) {
        return valuesByArea.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private static void applyBciStep1(final Map<String, Double> targetNetPositionInRegionByAreaId,
                                      final Map<String, Interval> regionFeasibilityRanges,
                                      final Map<String, Double> violationsByArea,
                                      final Map<String, Boolean> bciAppliedByArea,
                                      final double totalViolations,
                                      final double contraryViolations) {
        double totalShiftAvailableForHubsInMainViolation = computeTotalShiftAvailableForHubsInMainViolation(contraryViolations, totalViolations, targetNetPositionInRegionByAreaId, regionFeasibilityRanges, violationsByArea);

        for (Entry<String, Double> entry : violationsByArea.entrySet()) {
            String areaId = entry.getKey();
            double violation = entry.getValue();

            Interval interval = regionFeasibilityRanges.get(areaId);
            double frMin = interval.getMinValue();
            double frMax = interval.getMaxValue();

            if (isInContraryViolation(violation, totalViolations)) {
                double newNetPosition = contraryViolations > 0 ? frMax : frMin;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
                bciAppliedByArea.put(areaId, TRUE);
            } else if (isInMainViolation(violation, totalViolations)) {
                double netPosition = targetNetPositionInRegionByAreaId.get(areaId);
                double shiftAvailable = computeAvailableShiftWithContraryViolation(contraryViolations, interval, netPosition);
                double shiftApplied = contraryViolations * (shiftAvailable / totalShiftAvailableForHubsInMainViolation);
                double newNetPosition = netPosition + shiftApplied;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
                bciAppliedByArea.put(areaId, TRUE);
            }
        }
    }

    private static void applyBciStep2(final Map<String, Double> targetNetPositionInRegionByAreaId,
                                      final Map<String, Interval> regionFeasibilityRanges,
                                      final Map<String, Double> violationsByArea,
                                      final Map<String, Boolean> bciAppliedByArea,
                                      final double totalViolations) {
        double totalShiftAvailableForHubsNotInViolation = computeTotalShiftAvailableForHubsNotInViolation(totalViolations, violationsByArea, targetNetPositionInRegionByAreaId, regionFeasibilityRanges);

        double totalShiftToApplied = totalViolations < 0
            ? max(totalShiftAvailableForHubsNotInViolation, totalViolations)
            : min(totalShiftAvailableForHubsNotInViolation, totalViolations);

        for (Entry<String, Double> entry : violationsByArea.entrySet()) {
            String areaId = entry.getKey();
            double violation = entry.getValue();

            double netPosition = targetNetPositionInRegionByAreaId.get(areaId);
            Interval interval = regionFeasibilityRanges.get(areaId);
            double frMin = interval.getMinValue();
            double frMax = interval.getMaxValue();

            if (isInViolation(violation)) {
                double newNetPosition = totalViolations > 0 ? frMax : frMin;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
                bciAppliedByArea.put(areaId, TRUE);
            } else if (isNotNegligible(totalShiftAvailableForHubsNotInViolation)) {
                double shiftAvailable = computeAvailableShiftWithTotalViolation(totalViolations, interval, netPosition);
                double shiftApplied = totalShiftToApplied * (shiftAvailable / totalShiftAvailableForHubsNotInViolation);
                double newNetPosition = netPosition + shiftApplied;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
            }
        }
    }

    private static void applyBciStep3(final Map<String, Double> targetInRegionNpByArea,
                                      final Map<String, Interval> regionFeasibilityRanges,
                                      final double totalImbalance) {

        final double totalFeasibilityRanges = computeTotalFeasibilityRanges(regionFeasibilityRanges);

        targetInRegionNpByArea.keySet()
            .forEach(area -> targetInRegionNpByArea.compute(area, (areaId, np) ->
                np - (totalImbalance * regionFeasibilityRanges.get(areaId).getRange() / totalFeasibilityRanges)
            ));
    }

    private static Map<String, Double> computeViolationsByArea(final Map<String, Double> netPositionInRegionByArea,
                                                               final Map<String, Interval> regionFeasibilityRanges) {
        return netPositionInRegionByArea.entrySet().stream()
            .collect(toMap(
                Entry::getKey,
                entry -> {
                    String areaId = entry.getKey();
                    double netPosition = netPositionInRegionByArea.get(areaId);
                    Interval interval = regionFeasibilityRanges.get(areaId);
                    return netPosition - Math.clamp(netPosition, interval.getMinValue(), interval.getMaxValue());
                }
            ));
    }

    private static double computeContraryViolations(final Map<String, Double> violationsByArea,
                                                    final double totalViolations) {
        final ToDoubleFunction<Double> toContraryViolation = v -> totalViolations < 0 ? max(v, .0) : min(v, .0);
        return violationsByArea.values().stream().mapToDouble(toContraryViolation).sum();
    }

    private static double computeAvailableShiftWithContraryViolation(final double contraryViolation,
                                                                     final Interval feasibilityInterval,
                                                                     final double netPosition) {
        double frMin = feasibilityInterval.getMinValue();
        double frMax = feasibilityInterval.getMaxValue();
        return (contraryViolation < 0 ? frMax : frMin) - netPosition;
    }

    private static double computeAvailableShiftWithTotalViolation(final double totalViolation,
                                                                  final Interval feasibilityInterval,
                                                                  final double netPosition) {
        double frMin = feasibilityInterval.getMinValue();
        double frMax = feasibilityInterval.getMaxValue();
        return (totalViolation > 0 ? frMax : frMin) - netPosition;
    }

    private static double computeTotalShiftAvailableForHubsInMainViolation(final double contraryViolations,
                                                                           final double totalViolations,
                                                                           final Map<String, Double> netPositionInRegionByArea,
                                                                           final Map<String, Interval> regionFeasibilityRanges,
                                                                           final Map<String, Double> violationsByArea) {
        return netPositionInRegionByArea.entrySet().stream()
            .filter(entry -> isInMainViolation(violationsByArea.get(entry.getKey()), totalViolations))
            .mapToDouble(entry -> {
                String areaId = entry.getKey();
                double netPosition = entry.getValue();
                return computeAvailableShiftWithContraryViolation(contraryViolations,
                                                                  regionFeasibilityRanges.get(areaId),
                                                                  netPosition);
            })
            .sum();
    }

    private static double computeTotalShiftAvailableForHubsNotInViolation(final double totalViolations,
                                                                          final Map<String, Double> violationsByArea,
                                                                          final Map<String, Double> netPositionInRegionByArea,
                                                                          final Map<String, Interval> regionFeasibilityRanges) {
        return netPositionInRegionByArea.entrySet().stream()
            .filter(entry -> isNotInViolation(violationsByArea.get(entry.getKey())))
            .mapToDouble(entry -> {
                String areaId = entry.getKey();
                double netPosition = entry.getValue();
                return computeAvailableShiftWithTotalViolation(totalViolations,
                                                               regionFeasibilityRanges.get(areaId),
                                                               netPosition);
            })
            .sum();
    }

    private static double computeTotalFeasibilityRanges(final Map<String, Interval> regionFeasibilityRanges) {
        return regionFeasibilityRanges.values().stream()
            .mapToDouble(Interval::getRange)
            .sum();
    }

    private static boolean isInContraryViolation(final double violation, final double totalViolations) {
        return isInViolation(violation) && totalViolations * violation <= 0;
    }

    private static boolean isInMainViolation(final double violation, final double totalViolations) {
        return isInViolation(violation) && totalViolations * violation >= 0;
    }

    private static boolean isNotInViolation(final double violation) {
        return violation == 0;
    }

    private static boolean isInViolation(final double violation) {
        return violation != 0;
    }

}

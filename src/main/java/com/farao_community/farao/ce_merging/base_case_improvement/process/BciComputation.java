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
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BciComputation {
    private static final double EPSILON = 0.01;
    private static final Logger LOGGER = LoggerFactory.getLogger(BciComputation.class);

    private final RegionConfiguration region;
    private final ReferenceProgram referenceProgram;
    private Map<String, Double> forecastNetPositionInRegionByArea = new HashMap<>();
    private Map<String, Double> targetNetPositionInRegionByArea = new HashMap<>();
    private Map<String, Double> forecastGlobalNetPositionByArea = new HashMap<>();
    private Map<String, Double> targetGlobalNetPositionByArea = new HashMap<>();
    private final Map<String, Boolean> bciAppliedByArea = new HashMap<>();

    BciComputation(final RegionConfiguration region, final ReferenceProgram referenceProgram) {
        this.region = region;
        this.referenceProgram = referenceProgram;
    }

    public BciComputationResult run(final Map<String, Interval> regionFeasibilityRanges,
                                    final Map<String, Double> initialRegionNetPositions,
                                    final double valueAlbeThatShouldBeDisplayedInCgm,
                                    final double valueAldeThatShouldBeDisplayedInCgm) {
        Map<String, BciAreaResults> results;

        if (!validFeasibilityRanges(regionFeasibilityRanges)) {
            LOGGER.error("The feasibility ranges are not valid");
            throw new CeMergingException("The feasibility ranges are not valid");
        }

        // compute forecast net position inside region and exchange out region for each area
        forecastNetPositionInRegionByArea = referenceProgram.computeAllNetPositionsInRegion(region);
        forecastGlobalNetPositionByArea = referenceProgram.computeAllGlobalNetPositions(region);
        updateFroecastNetPositionsWithAlegro(forecastNetPositionInRegionByArea, forecastGlobalNetPositionByArea, valueAlbeThatShouldBeDisplayedInCgm, valueAldeThatShouldBeDisplayedInCgm);

        Map<String, Double> outNetPositionByArea = referenceProgram.computeAllNetPositionsOutRegion(region);

        // check net position in feasibility ranges
        if (validForecastNetPosition(forecastNetPositionInRegionByArea, regionFeasibilityRanges)) {
            LOGGER.info("All forecast net positions are in the feasibility ranges, Bci is not applied");
            targetNetPositionInRegionByArea = forecastNetPositionInRegionByArea;
            targetGlobalNetPositionByArea = forecastGlobalNetPositionByArea;
            results = createResults(regionFeasibilityRanges, initialRegionNetPositions);
            results.forEach((key, value) -> value.setBciApplied(Boolean.FALSE));
            return new BciComputationResult(false, false, results);
        } else {
            LOGGER.info("Not all forecast net positions are in the feasibility ranges, Bci will be applied");
            Pair<Boolean, Map<String, Double>> resultBci = applyBci(forecastNetPositionInRegionByArea, regionFeasibilityRanges, bciAppliedByArea);
            targetNetPositionInRegionByArea = resultBci.getRight();
            targetGlobalNetPositionByArea = targetNetPositionInRegionByArea.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> e.getValue() + outNetPositionByArea.get(e.getKey())));
            results = createResults(regionFeasibilityRanges, initialRegionNetPositions);
            return new BciComputationResult(true, resultBci.getLeft(), results);
        }
    }

    private void updateFroecastNetPositionsWithAlegro(final Map<String, Double> forecastNetPositionInRegionByArea,
                                                      final Map<String, Double> forecastGlobalNetPositionByArea,
                                                      final double valueAlbeThatShouldBeDisplayedInCgm,
                                                      final double valueAldeThatShouldBeDisplayedInCgm) {
        // Bci process should not take Alegro flows into account.
        // ForecastNetPositionInRegionByArea = BE-CORE from NPF file which contains already ALBE-CORE flow
        // That's why we must subtract from it valueXThatShouldBeDisplayedInCgm (= ALBE-CORE flow) to have only the AC target flow as the Bci target flow.
        forecastNetPositionInRegionByArea.put(region.getAreasIn().get("BE"), forecastNetPositionInRegionByArea.get(region.getAreasIn().get("BE")) - valueAlbeThatShouldBeDisplayedInCgm);
        forecastNetPositionInRegionByArea.put(region.getAreasIn().get("DE"), forecastNetPositionInRegionByArea.get(region.getAreasIn().get("DE")) - valueAldeThatShouldBeDisplayedInCgm);
        forecastGlobalNetPositionByArea.put(region.getAreasIn().get("BE"), forecastGlobalNetPositionByArea.get(region.getAreasIn().get("BE")) - valueAlbeThatShouldBeDisplayedInCgm);
        forecastGlobalNetPositionByArea.put(region.getAreasIn().get("DE"), forecastGlobalNetPositionByArea.get(region.getAreasIn().get("DE")) - valueAldeThatShouldBeDisplayedInCgm);
    }

    private Map<String, BciAreaResults> createResults(final Map<String, Interval> regionFeasibilityRanges,
                                                      final Map<String, Double> initialNetPositionInRegionByArea) {
        Map<String, BciAreaResults> resultsMap = region.getAreasIn().entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, entry -> this.createBciAreaResult(entry.getValue(), regionFeasibilityRanges, initialNetPositionInRegionByArea, bciAppliedByArea)));
        return new TreeMap<>(resultsMap);
    }

    private BciAreaResults createBciAreaResult(final String areaId,
                                               final Map<String, Interval> regionFeasibilityRanges,
                                               final Map<String, Double> initialNetPositionInRegionByArea,
                                               final Map<String, Boolean> bciAppliedByArea) {
        BciAreaResults.InRegionNetPositions inRegionResults = new BciAreaResults.InRegionNetPositions(
            initialNetPositionInRegionByArea.getOrDefault(areaId, 0.),
            regionFeasibilityRanges.get(areaId).getMinValue(),
            regionFeasibilityRanges.get(areaId).getMaxValue(),
            Math.min(regionFeasibilityRanges.get(areaId).getMinValue(), targetNetPositionInRegionByArea.get(areaId)),
            Math.max(regionFeasibilityRanges.get(areaId).getMaxValue(), targetNetPositionInRegionByArea.get(areaId)),
            forecastNetPositionInRegionByArea.get(areaId),
            targetNetPositionInRegionByArea.get(areaId));
        Boolean bciApplied = bciAppliedByArea.get(areaId);

        BciAreaResults.GlobalNetPositions globalResults = new BciAreaResults.GlobalNetPositions(
            forecastGlobalNetPositionByArea.get(areaId),
            targetGlobalNetPositionByArea.get(areaId));
        return new BciAreaResults(inRegionResults, globalResults, bciApplied);
    }

    private boolean validFeasibilityRanges(final Map<String, Interval> regionFeasibilityRanges) {
        return regionFeasibilityRanges.keySet().containsAll(region.getAreasIn().values());
    }

    private static boolean validForecastNetPosition(final Map<String, Double> netPositionInRegionByArea,
                                                    final Map<String, Interval> regionFeasibilityRanges) {
        boolean validForecastNetPositionFlag = Boolean.TRUE;
        for (Entry<String, Double> e : netPositionInRegionByArea.entrySet()) {
            String areaId = e.getKey();
            if (!regionFeasibilityRanges.get(areaId).containsValue(e.getValue())) {
                LOGGER.warn("Area: {} : Feasibility range interval is {}, but Forecast Net Position in region {} is outside interval", areaId, regionFeasibilityRanges.get(areaId).toString(), e.getValue());
                validForecastNetPositionFlag = Boolean.FALSE;
            } else {
                LOGGER.info("Area: {} : Feasibility range interval is {}, Forecast Net Position in region {} is inside interval", areaId, regionFeasibilityRanges.get(areaId).toString(), e.getValue());
            }
        }
        return validForecastNetPositionFlag;
    }

    private static boolean isDifferentFromZero(final double value) {
        BigDecimal bigDecimalValue = BigDecimal.valueOf(value);
        BigDecimal bigDecimalEpsilon = BigDecimal.valueOf(BciComputation.EPSILON);
        return bigDecimalValue.abs().compareTo(bigDecimalEpsilon) > 0;
    }

    private static Pair<Boolean, Map<String, Double>> applyBci(final Map<String, Double> refNetPositionInRegionByArea,
                                                               final Map<String, Interval> regionFeasibilityRanges,
                                                               final Map<String, Boolean> bciAppliedByArea) {
        Map<String, Double> targetNetPositionInRegionByAreaId = new HashMap<>(refNetPositionInRegionByArea);
        targetNetPositionInRegionByAreaId.keySet().forEach(areaId -> bciAppliedByArea.put(areaId, Boolean.FALSE));

        // Initial violation

        Map<String, Double> violationsByArea = computeViolationsByArea(targetNetPositionInRegionByAreaId, regionFeasibilityRanges);
        double totalViolations = violationsByArea.values().stream().mapToDouble(Double::doubleValue).sum();
        double contraryViolations = computeContraryViolations(violationsByArea, totalViolations);

        // Step 1, Preliminary BCI to solve contrary violations

        if (isDifferentFromZero(contraryViolations)) {
            LOGGER.info("Total violation {}, Contrary violations : {} , Prelimanry BC to solve contrary violations will be aplied", totalViolations, contraryViolations);
            applyBciStep1(targetNetPositionInRegionByAreaId, regionFeasibilityRanges, violationsByArea, bciAppliedByArea, totalViolations, contraryViolations);
            violationsByArea = computeViolationsByArea(targetNetPositionInRegionByAreaId, regionFeasibilityRanges);
            totalViolations = violationsByArea.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        // Step 2, solving of violations while respecting feasibility ranges

        if (isDifferentFromZero(totalViolations)) {
            LOGGER.info("Total violations after solving contrary  violation : {}, Normal BCI will be applied", totalViolations);
            applyBciStep2(targetNetPositionInRegionByAreaId, regionFeasibilityRanges, violationsByArea, bciAppliedByArea, totalViolations);
        }

        // Step 3, Compensation of remaining imbalance

        double totalImbalance = targetNetPositionInRegionByAreaId.values().stream().mapToDouble(Double::doubleValue).sum();

        boolean isExtendedBciFeasibilityRanges = false;

        if (isDifferentFromZero(totalImbalance)) {
            LOGGER.info("Total imbalance after BCI : {}, compensation of remaining imbalance will be applied", totalImbalance);
            applyBciStep3(targetNetPositionInRegionByAreaId, regionFeasibilityRanges, totalImbalance);
            isExtendedBciFeasibilityRanges = true;
        }

        return Pair.of(isExtendedBciFeasibilityRanges, targetNetPositionInRegionByAreaId);
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
                bciAppliedByArea.put(areaId, Boolean.TRUE);
            } else if (isInMainViolation(violation, totalViolations)) {
                double netPosition = targetNetPositionInRegionByAreaId.get(areaId);
                double shiftAvailable = computeAvailableShiftWithContraryViolation(contraryViolations, frMin, frMax, netPosition);
                double shiftApplied = contraryViolations * (shiftAvailable / totalShiftAvailableForHubsInMainViolation);
                double newNetPosition = netPosition + shiftApplied;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
                bciAppliedByArea.put(areaId, Boolean.TRUE);
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
            ? Math.max(totalShiftAvailableForHubsNotInViolation, totalViolations)
            : Math.min(totalShiftAvailableForHubsNotInViolation, totalViolations);

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
                bciAppliedByArea.put(areaId, Boolean.TRUE);
            } else if (isDifferentFromZero(totalShiftAvailableForHubsNotInViolation)) {
                double shiftAvailable = computeAvailableShiftWithTotalViolation(totalViolations, frMin, frMax, netPosition);
                double shiftApplied = totalShiftToApplied * (shiftAvailable / totalShiftAvailableForHubsNotInViolation);
                double newNetPosition = netPosition + shiftApplied;
                targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
            }
        }
    }

    private static void applyBciStep3(final Map<String, Double> targetNetPositionInRegionByAreaId,
                                      final Map<String, Interval> regionFeasibilityRanges,
                                      final double totalImbalance) {

        double totalFeasibilityRanges = computeTotalFeasibilityRanges(regionFeasibilityRanges);

        for (Entry<String, Double> entry : targetNetPositionInRegionByAreaId.entrySet()) {
            String areaId = entry.getKey();
            double netPosition = entry.getValue();

            Interval interval = regionFeasibilityRanges.get(areaId);
            double frMin = interval.getMinValue();
            double frMax = interval.getMaxValue();

            double shiftApplied = -totalImbalance * ((frMax - frMin) / totalFeasibilityRanges);
            double newNetPosition = netPosition + shiftApplied;
            targetNetPositionInRegionByAreaId.put(areaId, newNetPosition);
        }
    }

    private static Map<String, Double> computeViolationsByArea(final Map<String, Double> netPositionInRegionByArea,
                                                               final Map<String, Interval> regionFeasibilityRanges) {
        return netPositionInRegionByArea.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> {
                    String areaId = entry.getKey();
                    double netPosition = netPositionInRegionByArea.get(areaId);
                    Interval interval = regionFeasibilityRanges.get(areaId);
                    double min = interval.getMinValue();
                    double max = interval.getMaxValue();
                    return (netPosition < min) ? netPosition - min :
                        (netPosition > max) ? netPosition - max : 0.0;
                }
            ));
    }

    private static double computeContraryViolations(final Map<String, Double> violationsByArea, final double totalViolations) {
        return totalViolations < 0
            ? violationsByArea.values().stream().mapToDouble(violation -> Math.max(violation, 0.0)).sum()
            : violationsByArea.values().stream().mapToDouble(violation -> Math.min(violation, 0.0)).sum();
    }

    private static double computeAvailableShiftWithContraryViolation(final double contraryViolation,
                                                                     final double frMin,
                                                                     final double frMax,
                                                                     final double netPosition) {
        return contraryViolation < 0 ? frMax - netPosition : frMin - netPosition;
    }

    private static double computeAvailableShiftWithTotalViolation(final double totalViolation,
                                                                  final double frMin,
                                                                  final double frMax,
                                                                  final double netPosition) {
        return totalViolation > 0 ? frMax - netPosition : frMin - netPosition;
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

                Interval interval = regionFeasibilityRanges.get(areaId);
                double frMin = interval.getMinValue();
                double frMax = interval.getMaxValue();

                return computeAvailableShiftWithContraryViolation(contraryViolations, frMin, frMax, netPosition);
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

                Interval interval = regionFeasibilityRanges.get(areaId);
                double frMin = interval.getMinValue();
                double frMax = interval.getMaxValue();

                return computeAvailableShiftWithTotalViolation(totalViolations, frMin, frMax, netPosition);
            })
            .sum();
    }

    private static double computeTotalFeasibilityRanges(final Map<String, Interval> regionFeasibilityRanges) {
        return regionFeasibilityRanges.values().stream()
            .mapToDouble(interval -> interval.getMaxValue() - interval.getMinValue())
            .sum();
    }

    private static boolean isInContraryViolation(final double violation, final double totalViolations) {
        return totalViolations >= 0 && violation < 0 ||
               totalViolations < 0 && violation > 0;
    }

    private static boolean isInMainViolation(final double violation, final double totalViolations) {
        return totalViolations >= 0 && violation > 0 ||
               totalViolations < 0 && violation < 0;
    }

    private static boolean isNotInViolation(final double violation) {
        return violation == 0;
    }

    private static boolean isInViolation(final double violation) {
        return violation != 0;
    }

}

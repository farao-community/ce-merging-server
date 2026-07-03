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
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciComputation {
    private static final double EPSILON = 0.01;
    private static final Logger LOGGER = LoggerFactory.getLogger(BciComputation.class);

    private final RegionConfiguration regionConfiguration;
    private final ReferenceProgram referenceProgram;
    private final Map<String, Interval> feasibilityRanges;
    private FlowByAreaMap inRegionNpfByArea = new FlowByAreaMap();
    private FlowByAreaMap targetInRegionNpByArea = new FlowByAreaMap();
    private FlowByAreaMap globalNpfByArea = new FlowByAreaMap();
    private FlowByAreaMap targetGlobalNpByArea = new FlowByAreaMap();
    private FlowByAreaMap violationsByArea = new FlowByAreaMap();
    private final Map<String, Boolean> bciAppliedByArea = new HashMap<>();

    BciComputation(final RegionConfiguration regionConfiguration,
                   final ReferenceProgram referenceProgram,
                   final Map<String, Interval> feasibilityRanges) {
        this.regionConfiguration = regionConfiguration;
        this.referenceProgram = referenceProgram;

        if (feasibilityRanges.keySet().containsAll(regionConfiguration.getAreasIn().values())) {
            this.feasibilityRanges = feasibilityRanges;
        } else {
            LOGGER.error("The feasibility ranges are not valid");
            throw new CeMergingException("The feasibility ranges are not valid");
        }

    }

    public BciComputationResult run(final FlowByAreaMap initialRegionNetPositions,
                                    final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        final Map<String, BciAreaResults> results;

        // compute forecast net position inside regionConfiguration and exchange out regionConfiguration for each area
        inRegionNpfByArea = referenceProgram.computeAllNetPositionsInRegion(regionConfiguration);
        globalNpfByArea = referenceProgram.computeAllGlobalNetPositions(regionConfiguration);

        shiftNpfWithAlegro(alBeToCeFlow, alDeToCeFlow);

        final FlowByAreaMap outNetPositionByArea = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);

        // check net position in feasibility ranges
        if (isNpfValid()) {
            LOGGER.info("All forecast net positions are in the feasibility ranges, BCI is not applied");
            targetInRegionNpByArea = inRegionNpfByArea;
            targetGlobalNpByArea = globalNpfByArea;
            results = createResults(initialRegionNetPositions);
            results.forEach((key, value) -> value.setBciApplied(FALSE));
            return new BciComputationResult(false, false, results);
        } else {
            LOGGER.info("Not all forecast net positions are in the feasibility ranges, BCI will be applied");
            final Pair<Boolean, FlowByAreaMap> resultBci = applyBci();
            targetInRegionNpByArea = resultBci.getRight();
            targetGlobalNpByArea = targetInRegionNpByArea.withValuesShiftedBy(outNetPositionByArea::get);

            results = createResults(initialRegionNetPositions);
            return new BciComputationResult(true, resultBci.getLeft(), results);
        }
    }

    private Map<String, BciAreaResults> createResults(final FlowByAreaMap initialInRegionNpByArea) {
        return regionConfiguration.getAreasIn().entrySet().stream()
            .collect(
                toMap(Entry::getKey, entry -> this.createBciAreaResult(
                    entry.getValue(), initialInRegionNpByArea, bciAppliedByArea))
            );
    }

    private void shiftNpfWithAlegro(final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        // BCI process should not take Alegro flows into account.
        // inRegionNpfByArea = BE-CE from NPF file which alreadycontains ALBE-CE flow
        // That's why we must subtract by ALBE-CE flow to have only the AC target flow as the BCI target flow.
        final String belgium = regionConfiguration.getAreasIn().get("BE");
        final String germany = regionConfiguration.getAreasIn().get("DE");

        globalNpfByArea.shift(belgium, alBeToCeFlow);
        inRegionNpfByArea.shift(belgium, alBeToCeFlow);
        globalNpfByArea.shift(germany, alDeToCeFlow);
        inRegionNpfByArea.shift(germany, alDeToCeFlow);
    }

    private BciAreaResults createBciAreaResult(final String areaId,
                                               final FlowByAreaMap initialRegionNpByArea,
                                               final Map<String, Boolean> bciAppliedByArea) {
        final double targetRegionNp = targetInRegionNpByArea.get(areaId);
        final double regionMinFeasible = getMinConstraintOfArea(areaId);
        final double regionMaxFeasible = getMaxConstraintOfArea(areaId);

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

    private Pair<Boolean, FlowByAreaMap> applyBci() {
        solveContraryViolations();
        solveMainViolations();

        boolean hasBciExtendedRanges = !isNegligible(targetInRegionNpByArea.getTotal());
        if (hasBciExtendedRanges) {
            compensateRemainingImbalances();
        }

        return Pair.of(hasBciExtendedRanges, targetInRegionNpByArea);
    }

    private void solveContraryViolations() {
        computeViolationsByArea();

        if (isNegligible(getTotalContraryViolations())) {
            return;
        }
        final double shiftAvailable = computeTotalShiftAvailableFor(
            area -> isInMainViolation(violationsByArea.get(area)),
            this::computeAvailableShiftWithContraryViolation
        );

        violationsByArea.forEach(
            (area, violation) -> solveContraryViolationOrShiftNp(area, violation, shiftAvailable)
        );
    }

    private void solveContraryViolationOrShiftNp(final String areaId,
                                                 final double violation,
                                                 final double totalAvailableShift) {
        final double totalContraryViolations = getTotalContraryViolations();
        if (isInContraryViolation(violation)) {
            targetInRegionNpByArea.put(areaId, getMaxOrMinConstraint(areaId, totalContraryViolations > 0));
            bciAppliedByArea.put(areaId, TRUE);
        } else if (isInMainViolation(violation)) {
            double netPosition = targetInRegionNpByArea.get(areaId);
            double shiftAvailable = computeAvailableShiftWithContraryViolation(Map.entry(areaId, netPosition));
            double shiftApplied = totalContraryViolations * (shiftAvailable / totalAvailableShift);
            targetInRegionNpByArea.shift(areaId, shiftApplied);
            bciAppliedByArea.put(areaId, TRUE);
        }
    }

    private void solveMainViolations() {
        computeViolationsByArea();
        final double totalViolations = violationsByArea.getTotal();

        if (isNegligible(totalViolations)) {
            return;
        }

        final double shiftAvailable = computeTotalShiftAvailableFor(
            area -> isNotInViolation(violationsByArea.get(area)),
            this::computeAvailableShiftWithTotalViolation
        );

        final double totalShiftToApply = totalViolations < 0 ?
            max(shiftAvailable, totalViolations) : min(shiftAvailable, totalViolations);

        violationsByArea.forEach(
            (area, violation) -> solveMainViolationOrShiftNp(area, violation, shiftAvailable, totalShiftToApply)
        );
    }

    private void solveMainViolationOrShiftNp(final String areaId,
                                             final double violation,
                                             final double totalAvailableShift,
                                             final double totalShiftToApply) {
        final double totalViolations = violationsByArea.getTotal();
        if (isInViolation(violation)) {
            targetInRegionNpByArea.put(areaId, getMaxOrMinConstraint(areaId, totalViolations > 0));
            bciAppliedByArea.put(areaId, TRUE);
        } else if (!isNegligible(totalAvailableShift)) {
            double netPosition = targetInRegionNpByArea.get(areaId);
            double shiftAvailable = computeAvailableShiftWithTotalViolation(Map.entry(areaId, netPosition));
            double shiftApplied = totalShiftToApply * (shiftAvailable / totalAvailableShift);
            targetInRegionNpByArea.shift(areaId, shiftApplied);
        }
    }

    private void compensateRemainingImbalances() {
        final double totalImbalance = targetInRegionNpByArea.getTotal();

        final double totalFeasibilityRanges = feasibilityRanges.values().stream()
            .mapToDouble(Interval::getRange)
            .sum();

        targetInRegionNpByArea.shiftAllBy(areaId -> -totalImbalance
                                                    * feasibilityRanges.get(areaId).getRange()
                                                    / totalFeasibilityRanges);

    }

    private void computeViolationsByArea() {
        violationsByArea = targetInRegionNpByArea
            .withValuesShiftedBy(areaId -> {
                double netPosition = inRegionNpfByArea.get(areaId);
                Interval interval = feasibilityRanges.get(areaId);
                return -Math.clamp(netPosition, interval.getMinValue(), interval.getMaxValue());
            });
    }

     /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
         these do not modify class members
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private boolean isNpfValid() {
        return inRegionNpfByArea.entrySet().stream()
            .allMatch(e -> feasibilityRanges.get(e.getKey()).containsValue(e.getValue()));
    }

    private static boolean isNegligible(final double value) {
        return BigDecimal.valueOf(value).abs().compareTo(BigDecimal.valueOf(EPSILON)) == 0;
    }

    private double getMaxOrMinConstraint(final String areaId, final boolean maxIfTrue) {
        if (maxIfTrue) {
            return getMaxConstraintOfArea(areaId);
        } else {
            return getMinConstraintOfArea(areaId);
        }
    }

    private double getMinConstraintOfArea(final String areaId) {
        return feasibilityRanges.get(areaId).getMinValue();
    }

    private double getMaxConstraintOfArea(final String areaId) {
        return feasibilityRanges.get(areaId).getMaxValue();
    }

    private double getTotalContraryViolations() {
        final double totalViolations = violationsByArea.getTotal();
        final ToDoubleFunction<Double> toContraryViolation = v -> totalViolations < 0 ? max(v, .0) : min(v, .0);

        return violationsByArea.values().stream().mapToDouble(toContraryViolation).sum();
    }

    private double computeAvailableShiftWithContraryViolation(final Map.Entry<String, Double> netPositionByArea) {
        return getMaxOrMinConstraint(netPositionByArea.getKey(), getTotalContraryViolations() < 0)
               - netPositionByArea.getValue();
    }

    private double computeAvailableShiftWithTotalViolation(final Map.Entry<String, Double> netPositionByArea) {
        return getMaxOrMinConstraint(netPositionByArea.getKey(), violationsByArea.getTotal() > 0)
               - netPositionByArea.getValue();
    }

    private double computeTotalShiftAvailableFor(final Predicate<String> filterOnKey,
                                                 final ToDoubleFunction<Entry<String, Double>> computeUnitaryShift) {
        return targetInRegionNpByArea.entrySet().stream()
            .filter(entry -> filterOnKey.test(entry.getKey()))
            .mapToDouble(computeUnitaryShift)
            .sum();
    }

    private boolean isInContraryViolation(final double violation) {
        final double totalViolations = violationsByArea.getTotal();
        return isInViolation(violation) && totalViolations * violation <= 0;
    }

    private boolean isInMainViolation(final double violation) {
        final double totalViolations = violationsByArea.getTotal();
        return isInViolation(violation) && totalViolations * violation >= 0;
    }

    private boolean isNotInViolation(final double violation) {
        return violation == 0;
    }

    private static boolean isInViolation(final double violation) {
        return violation != 0;
    }

}

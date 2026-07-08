/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.GlobalNetPositions;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.InRegionNetPositions;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
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
    private static final BigDecimal EPSILON = BigDecimal.valueOf(0.01);
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

    public BciComputation(final RegionConfiguration regionConfiguration,
                          final ReferenceProgram referenceProgram,
                          final Map<String, Interval> feasibilityRanges) {
        this.regionConfiguration = regionConfiguration;
        this.referenceProgram = referenceProgram;

        // if every defined areaIn has a corresponding range
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

        inRegionNpfByArea = referenceProgram.computeAllNetPositionsInRegion(regionConfiguration);
        globalNpfByArea = referenceProgram.computeAllGlobalNetPositions(regionConfiguration);

        shiftNpfWithAlegro(alBeToCeFlow, alDeToCeFlow);

        final FlowByAreaMap outNetPositionByArea = referenceProgram.computeAllNetPositionsOutRegion(regionConfiguration);

        targetInRegionNpByArea = inRegionNpfByArea.copy();
        if (npfIsInFeasibilityRanges()) {
            LOGGER.info("All forecast net positions are in the feasibility ranges, BCI is not applied");
            targetGlobalNpByArea = globalNpfByArea.copy();
            results = createResults(initialRegionNetPositions);
            results.values().forEach(result -> result.setBciApplied(FALSE));
            return new BciComputationResult(false, false, results);
        } else {
            LOGGER.info("Not all forecast net positions are in the feasibility ranges, BCI will be applied");
            final boolean hasExtendedRanges = applyBci();
            targetGlobalNpByArea = targetInRegionNpByArea.withValuesShiftedBy(outNetPositionByArea::get);
            results = createResults(initialRegionNetPositions);
            return new BciComputationResult(true, hasExtendedRanges, results);
        }
    }

    private Map<String, BciAreaResults> createResults(final FlowByAreaMap initialInRegionNpByArea) {
        return regionConfiguration
            .getAreasIn()
            .entrySet()
            .stream()
            .collect(toMap(Entry::getKey,
                           entry -> this.createBciAreaResult(entry.getValue(),
                                                             initialInRegionNpByArea,
                                                             bciAppliedByArea)));
    }

    private void shiftNpfWithAlegro(final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        // BCI process should not take Alegro flows into account.
        // inRegionNpfByArea = BE-CE from NPF file which alreadycontains ALBE-CE flow
        // That's why we must subtract by ALBE-CE flow to have only the AC target flow as the BCI target flow.
        final String belgium = regionConfiguration.getAreaInEic("BE");
        final String germany = regionConfiguration.getAreaInEic("DE");

        globalNpfByArea.shiftFlow(belgium, alBeToCeFlow);
        inRegionNpfByArea.shiftFlow(belgium, alBeToCeFlow);
        globalNpfByArea.shiftFlow(germany, alDeToCeFlow);
        inRegionNpfByArea.shiftFlow(germany, alDeToCeFlow);
    }

    private BciAreaResults createBciAreaResult(final String areaId,
                                               final FlowByAreaMap initialRegionNpByArea,
                                               final Map<String, Boolean> bciAppliedByArea) {
        final double targetRegionNp = targetInRegionNpByArea.get(areaId);
        final double regionMinFeasible = getMinConstraintOfArea(areaId);
        final double regionMaxFeasible = getMaxConstraintOfArea(areaId);

        final InRegionNetPositions inRegionResults = new InRegionNetPositions(
            initialRegionNpByArea.getOrZero(areaId),
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

    private boolean applyBci() {
        inRegionNpfByArea.keySet().forEach(areaId -> bciAppliedByArea.put(areaId, FALSE));
        computeViolations();
        solveContraryViolations();
        solveMainViolations();

        boolean extendedRange = !isNegligible(targetInRegionNpByArea.getTotalFlow());
        if (extendedRange) {
            compensateRemainingImbalances();
        }

        return extendedRange;
    }

    private void solveContraryViolations() {
        if (isNegligible(getTotalContraryViolations())) {
            return;
        }
        final double shiftAvailable = computeTotalShiftAvailableFor(
            area -> isInMainViolation(violationsByArea.get(area)),
            this::getAvailableShiftWithContraryViolation
        );

        violationsByArea.forEach(
            (area, violation) -> solveContraryViolation(area, violation, shiftAvailable)
        );

        computeViolations();
    }

    private void solveContraryViolation(final String areaId,
                                        final double violation,
                                        final double totalAvailableShift) {
        final double totalContraryViolations = getTotalContraryViolations();
        if (isInContraryViolation(violation)) {
            targetInRegionNpByArea.put(areaId, getMaxOrMinConstraint(areaId, totalContraryViolations > 0));
            bciAppliedByArea.put(areaId, TRUE);
        } else if (isInMainViolation(violation)) {
            double shift = totalContraryViolations * (getAvailableShiftWithContraryViolation(areaId) / totalAvailableShift);
            targetInRegionNpByArea.shiftFlow(areaId, shift);
            bciAppliedByArea.put(areaId, TRUE);
        }
    }

    private void solveMainViolations() {
        final double totalViolations = violationsByArea.getTotalFlow();

        if (isNegligible(totalViolations)) {
            return;
        }

        final double shiftAvailable = computeTotalShiftAvailableFor(
            area -> !isInViolation(violationsByArea.get(area)),
            this::getAvailableShiftWithTotalViolation
        );

        final double totalShiftToApply = totalViolations < 0 ?
            max(shiftAvailable, totalViolations) : min(shiftAvailable, totalViolations);

        violationsByArea.forEach(
            (area, violation) -> solveMainViolation(area, violation, shiftAvailable, totalShiftToApply)
        );
    }

    private void solveMainViolation(final String areaId,
                                    final double violation,
                                    final double totalAvailableShift,
                                    final double totalShiftToApply) {
        final double totalViolations = violationsByArea.getTotalFlow();
        if (isInViolation(violation)) {
            targetInRegionNpByArea.put(areaId, getMaxOrMinConstraint(areaId, totalViolations > 0));
            bciAppliedByArea.put(areaId, TRUE);
        } else if (!isNegligible(totalAvailableShift)) {
            double shift = totalShiftToApply * (getAvailableShiftWithTotalViolation(areaId) / totalAvailableShift);
            targetInRegionNpByArea.shiftFlow(areaId, shift);
        }
    }

    private void compensateRemainingImbalances() {
        final double totalImbalance = targetInRegionNpByArea.getTotalFlow();

        final double totalFeasibilityRanges = feasibilityRanges.values().stream()
            .mapToDouble(Interval::getRange)
            .sum();

        targetInRegionNpByArea.shiftAllFlowsUsing(areaId -> -totalImbalance
                                                            * feasibilityRanges.get(areaId).getRange()
                                                            / totalFeasibilityRanges);

    }

    private void computeViolations() {
        violationsByArea = targetInRegionNpByArea.withValuesShiftedBy(
            area -> -Math.clamp(targetInRegionNpByArea.get(area),
                                getMinConstraintOfArea(area),
                                getMaxConstraintOfArea(area))
        );
    }

     /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
         these do not modify class members
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private boolean npfIsInFeasibilityRanges() {
        return inRegionNpfByArea.entrySet().stream()
            .allMatch(e -> feasibilityRanges.get(e.getKey()).hasWithinBounds(e.getValue()));
    }

    private static boolean isNegligible(final double value) {
        return BigDecimal.valueOf(value).abs().compareTo(EPSILON) <= 0;
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
        final double totalViolations = violationsByArea.getTotalFlow();
        final ToDoubleFunction<Double> toContraryViolation = v -> totalViolations < 0 ? max(v, 0.0) : min(v, 0.0);

        return violationsByArea.values().stream().mapToDouble(toContraryViolation).sum();
    }

    private double getAvailableShiftWithContraryViolation(final String area) {
        return getDistanceFromExtremum(area, getTotalContraryViolations() < 0);
    }

    private double getAvailableShiftWithTotalViolation(final String area) {
        return getDistanceFromExtremum(area, violationsByArea.getTotalFlow() > 0);
    }

    private double getDistanceFromExtremum(final String area, final boolean fromMaxIf) {
        return getMaxOrMinConstraint(area, fromMaxIf) - targetInRegionNpByArea.get(area);
    }

    private double computeTotalShiftAvailableFor(final Predicate<String> filterOnKey,
                                                 final ToDoubleFunction<String> valueMapper) {
        return targetInRegionNpByArea.keySet().stream()
            .filter(filterOnKey)
            .mapToDouble(valueMapper)
            .sum();
    }

    private boolean isInContraryViolation(final double violation) {
        final double totalViolations = violationsByArea.getTotalFlow();
        return totalViolations >= 0 && violation < 0
               || totalViolations < 0 && violation > 0;
    }

    private boolean isInMainViolation(final double violation) {
        final double totalViolations = violationsByArea.getTotalFlow();
        return totalViolations >= 0 && violation > 0
               || totalViolations < 0 && violation < 0;
    }

    private static boolean isInViolation(final double violation) {
        return violation != 0;
    }

}

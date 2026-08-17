/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.GlobalNetPositions;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.InRegionNetPositions;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumProperty;
import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumPropertyFiltered;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toMap;

public class BciComputer {
    private static final BigDecimal EPSILON = BigDecimal.valueOf(0.01);
    private static final Logger LOGGER = LoggerFactory.getLogger(BciComputer.class);

    private final RegionConfiguration regions;
    private final ReferenceProgram referenceProgram;
    private final Map<String, Interval> feasibilityRanges;

    private FlowByAreaMap inRegionNpfByArea = new FlowByAreaMap();
    private FlowByAreaMap targetInRegionNpByArea = new FlowByAreaMap();
    private FlowByAreaMap globalNpfByArea = new FlowByAreaMap();
    private FlowByAreaMap targetGlobalNpByArea = new FlowByAreaMap();
    private FlowByAreaMap violationsByArea = new FlowByAreaMap();
    private final Map<String, Boolean> bciAppliedByArea = new HashMap<>();

    public BciComputer(final RegionConfiguration regions,
                       final ReferenceProgram referenceProgram,
                       final Map<String, Interval> feasibilityRanges) {
        this.regions = regions;
        this.referenceProgram = referenceProgram;

        // if every defined areaIn has a corresponding range
        if (feasibilityRanges.keySet().containsAll(regions.getAreasIn().values())) {
            this.feasibilityRanges = feasibilityRanges;
        } else {
            final String errorMessage = "The feasibility ranges are invalid";
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }

    }

    public BciComputationResult run(final FlowByAreaMap initialRegionNetPositions,
                                    final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        final Map<String, BciAreaResults> results;
        inRegionNpfByArea = referenceProgram.getAllNetPositionsInRegion(regions);
        globalNpfByArea = referenceProgram.getAllGlobalNetPositions(regions);
        shiftNpfWithAlegro(alBeToCeFlow, alDeToCeFlow);
        targetInRegionNpByArea = inRegionNpfByArea.copy();

        if (isNpfInFeasibilityRanges()) {
            LOGGER.info("All forecast net positions are in the feasibility ranges, BCI will not be applied");
            targetGlobalNpByArea = globalNpfByArea.copy();
            results = createResults(initialRegionNetPositions);
            results.values().forEach(result -> result.setBciApplied(FALSE));
            return new BciComputationResult(false, false, results);
        } else {
            LOGGER.info("There are forecast net positions outside feasibility ranges, BCI will be applied");
            final boolean hasExtendedRanges = applyBci();
            final FlowByAreaMap outNetPositionByArea = referenceProgram.getAllNetPositionsOutRegion(regions);
            targetGlobalNpByArea = targetInRegionNpByArea.withValuesShiftedBy(outNetPositionByArea::getOrZero);
            results = createResults(initialRegionNetPositions);
            return new BciComputationResult(true, hasExtendedRanges, results);
        }
    }

    private Map<String, BciAreaResults> createResults(final FlowByAreaMap initialInRegionNpByArea) {
        return regions
            .getAreasIn()
            .entrySet()
            .stream()
            .collect(toMap(Entry::getKey, e -> createBciAreaResult(e.getValue(),
                                                                   initialInRegionNpByArea,
                                                                   bciAppliedByArea)));
    }

    private void shiftNpfWithAlegro(final double alBeToCeFlow,
                                    final double alDeToCeFlow) {
        // BCI process should not take Alegro flows into account.
        // inRegionNpfByArea = BE-CE from NPF file which alreadycontains ALBE-CE flow
        // That's why we must subtract by ALBE-CE flow to have only the AC target flow as the BCI target flow.
        final String belgium = regions.getAreaInEic("BE");
        final String germany = regions.getAreaInEic("DE");

        globalNpfByArea.shiftFlow(belgium, alBeToCeFlow);
        inRegionNpfByArea.shiftFlow(belgium, alBeToCeFlow);
        globalNpfByArea.shiftFlow(germany, alDeToCeFlow);
        inRegionNpfByArea.shiftFlow(germany, alDeToCeFlow);
    }

    private BciAreaResults createBciAreaResult(final String areaId,
                                               final FlowByAreaMap initialRegionNpByArea,
                                               final Map<String, Boolean> bciAppliedByArea) {
        final double targetRegionNp = targetInRegionNpByArea.get(areaId);
        final double regionMinFeasible = getMinConstraint(areaId);
        final double regionMaxFeasible = getMaxConstraint(areaId);

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

        if (!isNegligible(targetInRegionNpByArea.getTotalFlow())) {
            compensateRemainingImbalances();
            return true;
        }

        return false;
    }

    private void solveContraryViolations() {
        if (isNegligible(getTotalContraryViolations())) {
            return;
        }
        final double shiftAvailable = computeTotalShiftAvailableFor(
            area -> isInMainViolation(violationsByArea.get(area)),
            this::getAvailableShiftWithContraryViolation
        );

        violationsByArea.forEach((area, violation) -> solveContraryViolation(area, violation, shiftAvailable));

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

        violationsByArea.forEach((area, violation) -> solveMainViolation(area,
                                                                         violation,
                                                                         shiftAvailable,
                                                                         totalShiftToApply));
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

        final double totalFeasibilityRanges = sumProperty(feasibilityRanges.values(), Interval::getRange);

        if (totalFeasibilityRanges != 0) {
            targetInRegionNpByArea.shiftAllFlowsWith(areaId -> -totalImbalance
                                                               * feasibilityRanges.get(areaId).getRange()
                                                               / totalFeasibilityRanges);
        }

    }

    private void computeViolations() {
        violationsByArea = targetInRegionNpByArea.withValuesShiftedBy(
            area -> -Math.clamp(targetInRegionNpByArea.get(area), getMinConstraint(area), getMaxConstraint(area))
        );
    }

     /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
         these do not modify class members
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private boolean isNpfInFeasibilityRanges() {
        return inRegionNpfByArea.entrySet().stream()
            .allMatch(e -> feasibilityRanges.get(e.getKey()).hasWithinBounds(e.getValue()));
    }

    private static boolean isNegligible(final double flowInMW) {
        return BigDecimal.valueOf(flowInMW).abs().compareTo(EPSILON) <= 0;
    }

    private double getMaxOrMinConstraint(final String areaId, final boolean maxIfTrue) {
        if (maxIfTrue) {
            return getMaxConstraint(areaId);
        } else {
            return getMinConstraint(areaId);
        }
    }

    private double getMinConstraint(final String areaId) {
        return feasibilityRanges.get(areaId).getMinValue();
    }

    private double getMaxConstraint(final String areaId) {
        return feasibilityRanges.get(areaId).getMaxValue();
    }

    private double getTotalContraryViolations() {
        final double totalViolations = violationsByArea.getTotalFlow();
        final ToDoubleFunction<Double> toContraryViolation = v -> totalViolations < 0 ? max(v, 0.0) : min(v, 0.0);

        return sumProperty(violationsByArea.values(), toContraryViolation);
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

    private double computeTotalShiftAvailableFor(final Predicate<String> filterOnArea,
                                                 final ToDoubleFunction<String> areaToShift) {

        return sumPropertyFiltered(targetInRegionNpByArea.keySet(), areaToShift, filterOnArea);
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

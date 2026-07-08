/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.xsd.FeasibilityRangeConstraint;
import com.farao_community.farao.ce_merging.xsd.FeasibilityRangeDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval.infinity;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraints;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;

public class FeasibilityRangeCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeasibilityRangeCalculator.class);
    private final RegionConfiguration regionConfiguration;

    public FeasibilityRangeCalculator(final RegionConfiguration regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }

    public Map<String, Interval> getRegionFeasibilityRanges(final byte[] externalConstraints,
                                                            final OffsetDateTime targetDate,
                                                            final Map<String, Double> netPositions,
                                                            final byte[] feasibilityRange) {

        final Map<String, Interval> extConstraintsMap = calculateConstraints(externalConstraints,
                                                                             regionConfiguration,
                                                                             targetDate);
        if (isEmpty(feasibilityRange)) {
            return extConstraintsMap;
        } else {
            final Map<String, Interval> feasibilityRangeMap = importFeasibilityRangesFile(feasibilityRange,
                                                                                          netPositions);
            return computeFinalConstraints(extConstraintsMap, feasibilityRangeMap);
        }
    }

    private static Map<String, Interval> computeFinalConstraints(final Map<String, Interval> extConstraints,
                                                                 final Map<String, Interval> feasibilityRanges) {
        return extConstraints
            .entrySet()
            .stream()
            .collect(toMap(Map.Entry::getKey, joinedWithFeasibilityRange(feasibilityRanges)));
    }

    private static Function<Map.Entry<String, Interval>, Interval> joinedWithFeasibilityRange(final Map<String, Interval> ranges) {
        return extCt -> extCt.getValue().join(ranges.getOrDefault(extCt.getKey(), infinity()));
    }

    public Map<String, Interval> importFeasibilityRangesFile(byte[] feasibilityRange,
                                                             Map<String, Double> netPositionMap) {

        final FeasibilityRangeDocument document = JaxbUtils.readFromBytes(FeasibilityRangeDocument.class,
                                                                          feasibilityRange);

        final Map<String, Interval> feasibilityRangesMap = regionConfiguration
            .getAreasIn()
            .values()
            .stream()
            .collect(toMap(identity(), v -> infinity()));

        final Map<String, Interval> fromDocument = document
            .getConstraints()
            .getFeasibilityRangeConstraint()
            .stream()
            .collect(toMap(frc -> frc.getArea().getV(),
                           frc -> computeIntervalWithNetPositions(frc, netPositionMap)));

        feasibilityRangesMap.putAll(fromDocument);

        return feasibilityRangesMap;
    }

    private enum Type {
        ABSOLUTE,
        RELATIVE
    }

    private static Interval computeIntervalWithNetPositions(final FeasibilityRangeConstraint feasibilityRange,
                                                            final Map<String, Double> netPositionMap) {
        double max = feasibilityRange.getMax().getV().doubleValue();
        double min = feasibilityRange.getMin().getV().doubleValue();
        final String type = feasibilityRange.getType().getV();
        switch (Type.valueOf(type)) {
            case ABSOLUTE:
                return new Interval(min, max);
            case RELATIVE:
                final String area = feasibilityRange.getArea().getV();
                if (!netPositionMap.containsKey(area)) {
                    throw new CeMergingException("Error in feasibility range computation: Initial net position not found for area " + area);
                }
                final double initialNetPosition = netPositionMap.get(area);
                max += initialNetPosition;
                min += initialNetPosition;
                return new Interval(min, max);
            default:
                LOGGER.error("Feasibility range constraints type {} is not acceptable", type);
                throw new CeMergingException("Feasibility range constraints type " + type + " is not acceptable");
        }
    }
}

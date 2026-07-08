/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.Interval;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.xsd.FeasibilityRangeConstraint;
import com.farao_community.farao.ce_merging.xsd.FeasibilityRangeDocument;
import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.Interval.infinity;
import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraints;

public class FeasibilityRangeCalculator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeasibilityRangeCalculator.class);
    private final RegionConfiguration regionConfiguration;

    public FeasibilityRangeCalculator(final RegionConfiguration regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }

    public Map<String, Interval> getRegionFeasibilityRanges(byte[] externalConstraints, OffsetDateTime targetDate, Map<String, Double> netPositionsMap, byte[] feasibilityRange) {

        try {
            Map<String, Interval> extConstraintsMap = calculateConstraints(externalConstraints, regionConfiguration, targetDate);
            if (!ArrayUtils.isEmpty(feasibilityRange)) {
                Map<String, Interval> feasibilityRangeMap = importFeasibilityRangesFile(feasibilityRange, netPositionsMap);
                return computeFinalConstraints(extConstraintsMap, feasibilityRangeMap);
            } else {
                return extConstraintsMap;
            }
        } catch (Exception e) {
            String errorMessage = "Error during calculation feasibility ranges, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private Map<String, Interval> computeFinalConstraints(Map<String, Interval> extConstraintsMap, Map<String, Interval> feasibilityRangeMap) {
        Map<String, Interval> finalContraintsMap = new HashMap<>();
        for (Map.Entry<String, Interval> entry : extConstraintsMap.entrySet()) {
            try {
                Interval finalInterval = entry.getValue().join(feasibilityRangeMap.getOrDefault(entry.getKey(), infinity()));
                finalContraintsMap.put(entry.getKey(), finalInterval);
            } catch (ArithmeticException e) {
                LOGGER.error("Impossible to join intervals for area '{}' ", entry.getKey(), e);
                throw new CeMergingException("Error during calculation feasibility ranges : Impossible to join intervals for area " + entry.getKey(), e);
            }
        }
        return finalContraintsMap;
    }

    public Map<String, Interval> importFeasibilityRangesFile(byte[] feasibilityRange, Map<String, Double> netPositionMap) throws JAXBException, IOException {
        JAXBContext jaxbContext = JAXBContext.newInstance(FeasibilityRangeDocument.class);
        Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();
        FeasibilityRangeDocument document = (FeasibilityRangeDocument) jaxbMarshaller.unmarshal(ByteSource.wrap(feasibilityRange).openStream());

        Map<String, Interval> feasibilityRangesMap = new HashMap<>();
        Map<String, String> regionAreasIdByCountry = regionConfiguration.getAreasIn();
        regionAreasIdByCountry.values().forEach(v -> feasibilityRangesMap.put(v, infinity()));

        document.getConstraints().getFeasibilityRangeConstraint().forEach(feasibilityRangeConstraint -> {
            Interval newInterval = computeIntervalForFeasibilityRangeConstraint(feasibilityRangeConstraint, netPositionMap);
            feasibilityRangesMap.put(feasibilityRangeConstraint.getArea().getV(), newInterval);
        });
        return feasibilityRangesMap;
    }

    private enum Type {
        ABSOLUTE,
        RELATIVE
    }

    private Interval computeIntervalForFeasibilityRangeConstraint(FeasibilityRangeConstraint feasibilityRangeConstraint, Map<String, Double> netPositionMap) {
        double max = feasibilityRangeConstraint.getMax().getV().doubleValue();
        double min = feasibilityRangeConstraint.getMin().getV().doubleValue();
        switch (Type.valueOf(feasibilityRangeConstraint.getType().getV())) {
            case ABSOLUTE:
                return new Interval(min, max);
            case RELATIVE:
                String areaFeasibilityRange = feasibilityRangeConstraint.getArea().getV();
                if (!netPositionMap.containsKey(areaFeasibilityRange)) {
                    throw new CeMergingException("Error in feasibility range computation: Initial net position not found for area " + areaFeasibilityRange);
                }
                double initialNetPosition = netPositionMap.get(areaFeasibilityRange);
                max += initialNetPosition;
                min += initialNetPosition;
                return new Interval(min, max);
            default:
                LOGGER.error("Feasibility range constraints type {} is not acceptable", feasibilityRangeConstraint.getType().getV());
                throw new CeMergingException("Feasibility range constraints type " + feasibilityRangeConstraint.getType().getV() + " is not acceptable");
        }
    }
}

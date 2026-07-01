/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

public final class ExternalConstraintsImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalConstraintsImporter.class);

    private ExternalConstraintsImporter() {
    }

    public static Map<String, Interval> calculateConstraints(final byte[] externalConstraints,
                                                             final RegionConfiguration regionConfiguration,
                                                             final OffsetDateTime targetDate) {
        List<NetPositionConstraint> validIntervalConstraints = getNetPositionConstraints(externalConstraints, targetDate);
        Map<String, String> regionAreasIdByCountry = regionConfiguration.getAreasIn();
        List<ExternalConstraintsInputs> externalConstraintsTmp = new ArrayList<>();
        validIntervalConstraints.stream().filter(netPositionConstraint -> !(netPositionConstraint.getHub().equalsIgnoreCase(ALEGRO_BE_NODE_NAME) || netPositionConstraint.getHub().equalsIgnoreCase(ALEGRO_DE_NODE_NAME))).forEach(ec -> externalConstraintsTmp.add(new ExternalConstraintsInputs(regionAreasIdByCountry.get(ec.getTsoOrigin()), ec.getDirection(), ec.getValue().doubleValue())));
        Map<String, Interval> externalConstraintsMap = new HashMap<>();
        regionAreasIdByCountry.values().forEach(v -> externalConstraintsMap.put(v, Interval.allDoubles()));
        updateExternalConstraintsIntervals(externalConstraintsTmp, externalConstraintsMap);
        return externalConstraintsMap;
    }

    public static Map<String, Interval> calculateConstraintsForAlegro(final byte[] externalConstraints,
                                                                      final OffsetDateTime targetDate) {
        List<NetPositionConstraint> validIntervalConstraints = getNetPositionConstraints(externalConstraints, targetDate);
        List<ExternalConstraintsInputs> externalConstraintsTmp = new ArrayList<>();
        validIntervalConstraints.stream().filter(netPositionConstraint -> netPositionConstraint.getHub().equalsIgnoreCase(ALEGRO_BE_NODE_NAME) || netPositionConstraint.getHub().equalsIgnoreCase(ALEGRO_DE_NODE_NAME)).forEach(ec -> externalConstraintsTmp.add(
            new ExternalConstraintsInputs(ec.getHub().toUpperCase(), ec.getDirection(), ec.getValue().doubleValue())));
        if (externalConstraintsTmp.isEmpty()) {
            LOGGER.warn("BE_AL and DE_AL does not exists in the External constraints file for the chosen target date {}. The Ec values for BE_AL and DE_AL will be the infinity", targetDate);
        }
        Map<String, Interval> externalConstraintsMapAlegro = new HashMap<>();
        externalConstraintsMapAlegro.put(ALEGRO_BE_NODE_NAME, Interval.allDoubles());
        externalConstraintsMapAlegro.put(ALEGRO_DE_NODE_NAME, Interval.allDoubles());
        updateExternalConstraintsIntervals(externalConstraintsTmp, externalConstraintsMapAlegro);
        return externalConstraintsMapAlegro;
    }

    private static List<NetPositionConstraint> getNetPositionConstraints(final byte[] externalConstraints,
                                                                         final OffsetDateTime targetDate) {
        try {
            LOGGER.info("Importing external constraints file");
            JAXBContext jaxbContext = JAXBContext.newInstance(FlowBasedExternalConstraintDocument.class);
            Unmarshaller jaxbMarshaller = jaxbContext.createUnmarshaller();
            FlowBasedExternalConstraintDocument document = (FlowBasedExternalConstraintDocument) jaxbMarshaller.unmarshal(ByteSource.wrap(externalConstraints).openStream());
            String timeInterval = document.getExternalConstraintTimeInterval().getV();
            if (!isWithinRange(targetDate, timeInterval)) {
                String errorMessage = "External constraints time interval: " + timeInterval + " does not include the process target date: " + targetDate;
                LOGGER.error(errorMessage);
                throw new CeMergingException(errorMessage);
            }
            List<NetPositionConstraint> validIntervalConstraints = new ArrayList<>();
            document.getConstraints().getNetPositionConstraint().forEach(netPositionConstraint -> {
                if (isWithinRange(targetDate, netPositionConstraint.getTimeInterval().getV())) {
                    validIntervalConstraints.add(netPositionConstraint);
                }
            });
            return validIntervalConstraints;
        } catch (final Exception e) {
            String errorMessage = "Couldn't import external constraints file, cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private static void updateExternalConstraintsIntervals(final List<ExternalConstraintsInputs> externalConstraintsTmp,
                                                           final Map<String, Interval> externalConstraintsMap) {
        externalConstraintsTmp.forEach(ec -> {
            final Interval interval = externalConstraintsMap.get(ec.getAreaId());
            switch (ec.getDirection()) {
                case "EXPORT":
                    interval.setMaxValue(ec.getValue());
                    break;
                case "IMPORT":
                    interval.setMinValue(-ec.getValue());
                    break;
                default:
                    LOGGER.error("External constraints direction {} is not acceptable", ec.getDirection());
                    throw new CeMergingException("External constraints direction " + ec.getDirection() + " is not acceptable");
            }

            externalConstraintsMap.put(ec.getAreaId(), interval);
        });
    }

    private static boolean isWithinRange(final OffsetDateTime targetDate,
                                         final String timeInterval) {
        final List<String> externalConstraintsDates = Arrays.asList(timeInterval.split("/"));
        final OffsetDateTime start = OffsetDateTime.parse(externalConstraintsDates.get(0), ISO_DATE_TIME);
        final OffsetDateTime end = OffsetDateTime.parse(externalConstraintsDates.get(1), ISO_DATE_TIME);
        return !targetDate.isBefore(start) && !targetDate.isAfter(end);
    }
}

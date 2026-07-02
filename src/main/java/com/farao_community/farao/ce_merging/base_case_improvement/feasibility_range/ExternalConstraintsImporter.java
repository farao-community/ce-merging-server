/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.FlowBasedExternalConstraintDocument;
import com.farao_community.farao.ce_merging.xsd.NetPositionConstraint;
import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.ExternalConstraintsInputs.fromNetPositionConstraint;
import static com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range.Interval.infinity;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_DE_NODE_NAME;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

public final class ExternalConstraintsImporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalConstraintsImporter.class);

    private ExternalConstraintsImporter() {
    }

    public static Map<String, Interval> calculateConstraints(final byte[] externalConstraints,
                                                             final RegionConfiguration regionConfiguration,
                                                             final OffsetDateTime targetDate) {
        final Map<String, String> regionAreasIdByCountry = regionConfiguration.getAreasIn();

        final List<ExternalConstraintsInputs> externalConstraintsTmp = getNetPositionConstraints(externalConstraints, targetDate)
            .stream()
            .filter(not(constrainsAlegro()))
            .map(npc -> {
                final ExternalConstraintsInputs eci = fromNetPositionConstraint(npc);
                eci.setAreaId(regionAreasIdByCountry.get(npc.getTsoOrigin()));
                return eci;
            })
            .toList();

        final Map<String, Interval> externalConstraintsMap = regionAreasIdByCountry.values()
            .stream()
            .collect(toMap(identity(), c -> infinity()));

        updateExternalConstraintsIntervals(externalConstraintsTmp, externalConstraintsMap);
        return externalConstraintsMap;
    }

    public static Map<String, Interval> calculateConstraintsForAlegro(final byte[] externalConstraints,
                                                                      final OffsetDateTime targetDate) {

        final List<ExternalConstraintsInputs> externalConstraintsTmp = getNetPositionConstraints(externalConstraints, targetDate)
            .stream()
            .filter(constrainsAlegro())
            .map(ExternalConstraintsInputs::fromNetPositionConstraint)
            .toList();

        if (externalConstraintsTmp.isEmpty()) {
            LOGGER.warn("BE_AL and DE_AL do not exist in the external constraints file for the chosen target date {}. The EC values for BE_AL and DE_AL will be infinite", targetDate);
        }

        final Map<String, Interval> alegroConstraints = new HashMap<>();
        alegroConstraints.put(ALEGRO_BE_NODE_NAME, infinity());
        alegroConstraints.put(ALEGRO_DE_NODE_NAME, infinity());
        updateExternalConstraintsIntervals(externalConstraintsTmp, alegroConstraints);
        return alegroConstraints;
    }

    static Predicate<NetPositionConstraint> constrainsAlegro() {
        return npc -> npc.getHub().equalsIgnoreCase(ALEGRO_BE_NODE_NAME)
                      || npc.getHub().equalsIgnoreCase(ALEGRO_DE_NODE_NAME);
    }

    private static List<NetPositionConstraint> getNetPositionConstraints(final byte[] externalConstraints,
                                                                         final OffsetDateTime targetDate) {
        try {
            LOGGER.info("Importing external constraints file");
            final FlowBasedExternalConstraintDocument document = (FlowBasedExternalConstraintDocument) JAXBContext
                .newInstance(FlowBasedExternalConstraintDocument.class)
                .createUnmarshaller()
                .unmarshal(ByteSource.wrap(externalConstraints).openStream());

            final String timeInterval = document.getExternalConstraintTimeInterval().getV();

            if (!isWithinRange(targetDate, timeInterval)) {
                final String errorMessage = "External constraints time interval %s does not include the process target date %s "
                    .formatted(timeInterval, targetDate);
                LOGGER.error(errorMessage);
                throw new CeMergingException(errorMessage);
            }

            return document.getConstraints()
                .getNetPositionConstraint()
                .stream()
                .filter(ct -> isWithinRange(targetDate, ct.getTimeInterval().getV()))
                .toList();

        } catch (final Exception e) {
            String errorMessage = "Couldn't import external constraints file";
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
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
                    final String errorMessage = "External constraints direction %s is not acceptable"
                        .formatted(ec.getDirection());
                    LOGGER.error(errorMessage);
                    throw new CeMergingException(errorMessage);
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

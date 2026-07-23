/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.forecast_netpositions;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.xsd.forecast_netpositions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public final class ForecastNetPositionImporter {
    private static final String A95_REASON_CODE = "A95";
    private static final String CONSTANT_RESOLUTION_CURVE_TYPE = "A01";
    private static final String VARIABLE_RESOLUTION_CURVE_TYPE = "A03";

    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastNetPositionImporter.class);

    public static ReferenceProgram importFromFile(final String netPositionFilePath, final OffsetDateTime targetDateTime) {
        final ReportingInformationMarketDocument document = JaxbUtils.readFromPath(ReportingInformationMarketDocument.class, netPositionFilePath);
        if (!isValidDocumentInterval(document, targetDateTime)) {
            LOGGER.error("Net position file is not valid for this date {}", targetDateTime);
            throw new CeMergingException("Net position file is not valid for this date " + targetDateTime);
        }
        final List<ReferenceExchangeData> exchangeDataList = document.getTimeSeries().stream()
                .filter(timeSeries -> isUsedForTheMerging(timeSeries))
                .map(timeSeries -> new ReferenceExchangeData(
                        timeSeries.getOutDomainMRID().getValue(),
                        timeSeries.getInDomainMRID().getValue(),
                        getFlow(targetDateTime, timeSeries)))
                .toList();
        LOGGER.info("net position file '{}' has been imported", netPositionFilePath);
        final String dailyInterval = getDailyInterval(document);
        return new ReferenceProgram(dailyInterval, targetDateTime, exchangeDataList);

    }

    private static String getDailyInterval(final ReportingInformationMarketDocument document) {
        return document.getTimePeriodTimeInterval().getStart() + "/" + document.getTimePeriodTimeInterval().getEnd();
    }

    private static boolean isUsedForTheMerging(final TimeSeries timeSeries) {
        return timeSeries.getReason().stream()
                .noneMatch(reason -> A95_REASON_CODE.equals(reason.getCode()));
    }

    private static double getFlow(final OffsetDateTime dateTime, final TimeSeries timeSeries) {
        return timeSeries.getPeriod().stream()
                .filter(period -> isValidPeriodInterval(period, dateTime))
                .findFirst()
                .map(period -> getFlowFromPeriod(dateTime, period, timeSeries.getCurveType()))
                .orElseGet(() -> {
                    LOGGER.warn("Flow value between {} and {} is not found for this date {}", timeSeries.getOutDomainMRID().getValue(), timeSeries.getInDomainMRID().getValue(), dateTime);
                    return 0.0;
                });

    }

    private static boolean isValidPeriodInterval(SeriesPeriod seriesPeriod, OffsetDateTime dateTime) {
        OffsetDateTime startDateTime = parseDateTime(seriesPeriod.getTimeInterval().getStart());
        OffsetDateTime endDateTime = parseDateTime(seriesPeriod.getTimeInterval().getEnd());
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static boolean isValidDocumentInterval(final ReportingInformationMarketDocument document, final OffsetDateTime dateTime) {
        if (document.getTimePeriodTimeInterval() == null) {
            throw new CeMergingException("Cannot import net position forecast file: missing time interval");
        }
        final OffsetDateTime startDateTime = parseDateTime(document.getTimePeriodTimeInterval().getStart());
        final OffsetDateTime endDateTime = parseDateTime(document.getTimePeriodTimeInterval().getEnd());
        return !dateTime.isBefore(startDateTime) && dateTime.isBefore(endDateTime);
    }

    private static double getFlowFromPeriod(final OffsetDateTime dateTime, final SeriesPeriod period, final String curveType) {
        final Duration resolution = period.getResolution();
        final long resolutionInSeconds = getResolutionInSeconds(resolution);
        final OffsetDateTime startDateTime = parseDateTime(period.getTimeInterval().getStart());
        final OffsetDateTime endDateTime = parseDateTime(period.getTimeInterval().getEnd());
        final List<Point> points = period.getPoint();
        return switch (curveType) {
            case CONSTANT_RESOLUTION_CURVE_TYPE -> getFlowFromConstantResolutionCurve(dateTime, startDateTime, resolutionInSeconds, points);
            case VARIABLE_RESOLUTION_CURVE_TYPE -> getFlowFromVariableResolutionCurve(dateTime, startDateTime, endDateTime, resolutionInSeconds, points);
            default -> {
                LOGGER.error("CurveType {} is not supported for net position forecast file", curveType);
                throw new CeMergingException("CurveType " + curveType + " is not supported for net position forecast file");
            }
        };
    }

    private static long getResolutionInSeconds(final Duration resolution) {
        return resolution.getHours() * 3600 + resolution.getMinutes() * 60 + (long) resolution.getSeconds();
    }

    private static double getFlowFromConstantResolutionCurve(final OffsetDateTime dateTime, OffsetDateTime startDateTime, final long resolutionInSeconds, final List<Point> points) {
        //The curve is made of successive Intervals of time (Blocks) of constant duration (size),
        // where the size of the Blocks is equal to the Resolution of the Period
        int increment = 0;
        OffsetDateTime currentStartDateTime = startDateTime;
        while (increment < points.size()) {
            final Point actualPoint = points.get(increment);
            final OffsetDateTime intervalEnd = currentStartDateTime.plus(resolutionInSeconds, ChronoUnit.SECONDS);
            if (dateTime.isBefore(intervalEnd)) {
                return actualPoint.getQuantity().doubleValue();
            }
            increment++;
            currentStartDateTime = intervalEnd;
        }

        return 0.0;
    }

    private static double getFlowFromVariableResolutionCurve(final OffsetDateTime dateTime, OffsetDateTime startDateTime, final OffsetDateTime endDateTime, final long resolutionInSeconds, final List<Point> points) {
        //The curve is made of successive Intervals of time (Blocks) of variable duration (size),
        //where the end date and end time of each Block are equal to the start date and start time of the next Interval.
        // For the last Block the end date and end time of the last Interval would be equal to EndDateTime of TimeInterval
        int increment = 0;
        while (increment < points.size() - 1) {
            final Point actualPoint = points.get(increment);
            final Point nextPoint = points.get(increment + 1);
            final OffsetDateTime intervalEnd = startDateTime.plus((nextPoint.getPosition() - 1) * resolutionInSeconds, ChronoUnit.SECONDS);
            if (dateTime.isBefore(intervalEnd)) {
                return actualPoint.getQuantity().doubleValue();
            }
            increment++;
        }
        if (dateTime.isBefore(endDateTime)) {
            return points.get(points.size() - 1).getQuantity().doubleValue();
        }
        return 0.0;
    }

    private static OffsetDateTime parseDateTime(final String dateTime) {
        return OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
    }
}

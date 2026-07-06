/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.inputs;

import com.farao_community.farao.ce_merging.common.config.IRegionConfiguration;
import com.farao_community.farao.ce_merging.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.base_case_improvement.data.FlowByAreaMap.toFlowByAreaMap;
import static java.util.function.Function.identity;

public class ReferenceProgram implements Serializable {

    private String dailyTimeInterval;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime targetDateTime;
    private List<ReferenceExchangeData> referenceExchangeDataList;

    @JsonCreator
    public ReferenceProgram(@JsonProperty("timeInterval") final String dailyTimeInterval,
                            @JsonProperty("targetDateTime") final OffsetDateTime targetDateTime,
                            @JsonProperty("referenceExchangeData") final List<ReferenceExchangeData> referenceExchangeDataList) {
        this.dailyTimeInterval = dailyTimeInterval;
        this.targetDateTime = targetDateTime;
        this.referenceExchangeDataList = referenceExchangeDataList;
    }

    public FlowByAreaMap computeGlobalNetPositionsForOutAreas(final IRegionConfiguration region) {
        final Predicate<String> isNotRegionOrItsAreasIn = areaId -> !region.getId().equals(areaId)
                                                                  && !region.getAreasIn().containsValue(areaId);
        return Stream
            .concat(referenceExchangeDataList.stream().map(ReferenceExchangeData::getAreaInId),
                    referenceExchangeDataList.stream().map(ReferenceExchangeData::getAreaOutId))
            .distinct()
            .filter(isNotRegionOrItsAreasIn)
            .collect(toFlowByAreaMap(identity(), this::getAreaGlobalNetPosition));
    }

    public FlowByAreaMap computeAllNetPositionsInRegion(final IRegionConfiguration region) {
        return computeForAllAreasIn(region, areaId -> getAreaNetPositionInRegion(areaId, region));
    }

    public FlowByAreaMap computeAllNetPositionsOutRegion(final IRegionConfiguration region) {
        return computeForAllAreasIn(region, areaId -> getAreaNetPositionOutRegion(areaId, region));
    }

    public FlowByAreaMap computeAllGlobalNetPositions(final IRegionConfiguration region) {
        return computeForAllAreasIn(region, this::getAreaGlobalNetPosition);
    }

    double getAreaGlobalNetPosition(final String areaId) {
        final double leavingArea = sumFlowsGiven(exc -> exc.comesFrom(areaId));
        final double enteringArea = sumFlowsGiven(exc -> exc.goesTo(areaId));
        return leavingArea - enteringArea;
    }

    double getAreaNetPositionInRegion(final String areaId, final IRegionConfiguration region) {
        // compute the netPosition of an area relative to a region
        final double enteringRegion = sumFlowsGiven(exc -> exc.flowsBetween(areaId, region.getId()));
        final double leavingRegion = sumFlowsGiven(exc -> exc.flowsBetween(region.getId(), areaId));
        return enteringRegion - leavingRegion;
    }

    double getAreaNetPositionOutRegion(final String areaId, final IRegionConfiguration region) {
        // compute the exchange of an area out of region
        final double outOfAreaToElsewhere = sumFlowsGiven(exc -> exc.comesFrom(areaId)
                                                                 && !exc.goesTo(region.getId()));

        final double intoAreaFromElsewhere = sumFlowsGiven(exc -> exc.goesTo(areaId)
                                                                  && !exc.comesFrom(region.getId()));
        return outOfAreaToElsewhere - intoAreaFromElsewhere;
    }

    private double sumFlowsGiven(final Predicate<ReferenceExchangeData> condition) {
        return referenceExchangeDataList.stream()
            .filter(condition)
            .mapToDouble(ReferenceExchangeData::getFlow)
            .sum();
    }

    private FlowByAreaMap computeForAllAreasIn(final IRegionConfiguration region,
                                               final Function<String, Double> areaToNetPosition) {
        return region.getAreasIn().values()
            .stream()
            .collect(toFlowByAreaMap(identity(), areaToNetPosition));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                            ACCESSORS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    public String getDailyTimeInterval() {
        return dailyTimeInterval;
    }

    public void setDailyTimeInterval(final String dailyTimeInterval) {
        this.dailyTimeInterval = dailyTimeInterval;
    }

    public OffsetDateTime getTargetDateTime() {
        return targetDateTime;
    }

    public void setTargetDateTime(final OffsetDateTime targetDateTime) {
        this.targetDateTime = targetDateTime;
    }

    public ReferenceProgram(final List<ReferenceExchangeData> referenceExchangeDataList) {
        this(null, null, referenceExchangeDataList);
    }

    public List<ReferenceExchangeData> getReferenceExchangeDataList() {
        return referenceExchangeDataList;
    }

    public void setReferenceExchangeDataList(final List<ReferenceExchangeData> referenceExchangeDataList) {
        this.referenceExchangeDataList = referenceExchangeDataList;
    }
}

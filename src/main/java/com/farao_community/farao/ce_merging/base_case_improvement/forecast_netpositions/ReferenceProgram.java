/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.forecast_netpositions;

import com.farao_community.farao.ce_merging.base_case_improvement.RegionConfiguration;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    double getAreaGlobalNetPosition(final String areaId) {
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaOutId().equals(areaId))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaInId().equals(areaId))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    double getAreaNetPositionInRegion(final String areaId, final RegionConfiguration region) {
        // compute the netPosition of an area relative to a region
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
                .filter(referenceExchange -> referenceExchange.isAreaOutToAreaInExchange(areaId, region.getId()))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
                .filter(referenceExchange -> referenceExchange.isAreaOutToAreaInExchange(region.getId(), areaId))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    double getAreaNetPositionOutRegion(final String areaId, final RegionConfiguration region) {
        // compute the exchange of an area out of region
        double netPosition = 0.;
        netPosition += referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaOutId().equals(areaId)
                        && !region.getId().equals(referenceExchangeData.getAreaInId()))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        netPosition -= referenceExchangeDataList.stream()
                .filter(referenceExchangeData -> referenceExchangeData.getAreaInId().equals(areaId)
                        && !region.getId().equals(referenceExchangeData.getAreaOutId()))
                .mapToDouble(ReferenceExchangeData::getFlow).sum();
        return netPosition;
    }

    public Map<String, Double> computeAllNetPositionsInRegion(final RegionConfiguration region) {
        return region.getAreasIn().values().stream().collect(Collectors.toMap(Function.identity(), areaId -> getAreaNetPositionInRegion(areaId, region)));
    }

    public Map<String, Double> computeAllNetPositionsOutRegion(final RegionConfiguration region) {
        Map<String, Double> outNetPositionByArea = new HashMap<>();
        region.getAreasIn().values().forEach(areaId -> outNetPositionByArea.put(areaId, this.getAreaNetPositionOutRegion(areaId, region)));
        return outNetPositionByArea;
    }

    public Map<String, Double> computeAllGlobalNetPositions(final RegionConfiguration region) {
        return region.getAreasIn().values().stream().collect(Collectors.toMap(Function.identity(), this::getAreaGlobalNetPosition));
    }

    public Map<String, Double> computeGlobalNetPositionsForOutAreas(final RegionConfiguration region) {
        List<String> outRegionAreas = Stream.concat(referenceExchangeDataList.stream().map(ReferenceExchangeData::getAreaInId),
                        referenceExchangeDataList.stream().map(ReferenceExchangeData::getAreaOutId))
                .distinct()
                .filter(areaId -> !region.getId().equals(areaId) && !region.getAreasIn().containsValue(areaId))
                .collect(Collectors.toList());
        return outRegionAreas.stream().collect(Collectors.toMap(Function.identity(), this::getAreaGlobalNetPosition));
    }

}

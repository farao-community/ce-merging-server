/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.forecast_netpositions;

import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

public class ReferenceProgram implements Serializable {

    private String dailyTimeInterval;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime targetDateTime;
    private List<ReferenceExchangeData> referenceExchangeDataList;

    @JsonCreator
    public ReferenceProgram(@JsonProperty("dailyTimeInterval") String dailyTimeInterval,
                            @JsonProperty("targetDateTime") OffsetDateTime targetDateTime,
                            @JsonProperty("referenceExchangeData") List<ReferenceExchangeData> referenceExchangeDataList) {
        this.dailyTimeInterval = dailyTimeInterval;
        this.targetDateTime = targetDateTime;
        this.referenceExchangeDataList = Objects.requireNonNull(referenceExchangeDataList);
    }

    public String getDailyTimeInterval() {
        return dailyTimeInterval;
    }

    public void setDailyTimeInterval(String dailyTimeInterval) {
        this.dailyTimeInterval = dailyTimeInterval;
    }

    public OffsetDateTime getTargetDateTime() {
        return targetDateTime;
    }

    public void setTargetDateTime(OffsetDateTime targetDateTime) {
        this.targetDateTime = targetDateTime;
    }

    public ReferenceProgram(List<ReferenceExchangeData> referenceExchangeDataList) {
        this(null, null, referenceExchangeDataList);
    }

    public List<ReferenceExchangeData> getReferenceExchangeDataList() {
        return referenceExchangeDataList;
    }

    public void setReferenceExchangeDataList(List<ReferenceExchangeData> referenceExchangeDataList) {
        this.referenceExchangeDataList = Objects.requireNonNull(referenceExchangeDataList);
    }
}

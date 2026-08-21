/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci;

import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeSerializer;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroData;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public class JsonBciOutputStructure {

    @JsonProperty("processDateTime")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime processDateTime;

    @JsonProperty("regionName")
    private String regionName;

    @JsonProperty("bciComputationResult")
    private JsonBciComputationResult jsonBciComputationResult;

    @JsonProperty("outRegionResults")
    private JsonOutRegionResults jsonOutRegionResults;

    @JsonProperty("bciAlegroData")
    private BciAlegroData bciAlegroData;

    public OffsetDateTime getProcessDateTime() {
        return processDateTime;
    }

    public String getRegionName() {
        return regionName;
    }

    public JsonBciComputationResult getJsonBciComputationResult() {
        return jsonBciComputationResult;
    }

    public JsonOutRegionResults getJsonOutRegionResults() {
        return jsonOutRegionResults;
    }

    public BciAlegroData getBciAlegroData() {
        return bciAlegroData;
    }
}


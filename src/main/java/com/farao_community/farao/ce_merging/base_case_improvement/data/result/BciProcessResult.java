/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.result;

import com.farao_community.farao.ce_merging.base_case_improvement.data.alegro.BciAlegroData;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

public class BciProcessResult {
    private final String regionName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private final OffsetDateTime processDateTime;

    private final BciComputationResult bciComputationResult;
    private final OutRegionResults outRegionResults;
    private final BciAlegroData bciAlegroData;

    @JsonCreator
    public BciProcessResult(@JsonProperty("region") final String regionName,
                            @JsonProperty("processDateTime") final OffsetDateTime processDateTime,
                            @JsonProperty("BciResults") final BciComputationResult bciComputationResult,
                            @JsonProperty("OutRegionResults") final OutRegionResults outRegionResults,
                            @JsonProperty("AlegroData") final BciAlegroData bciAlegroData) {
        this.regionName = regionName;
        this.processDateTime = processDateTime;
        this.bciComputationResult = bciComputationResult;
        this.outRegionResults = outRegionResults;
        this.bciAlegroData = bciAlegroData;
    }

    public String getRegionName() {
        return regionName;
    }

    public OffsetDateTime getProcessDateTime() {
        return processDateTime;
    }

    public BciComputationResult getBciComputationResult() {
        return bciComputationResult;
    }

    public OutRegionResults getOutRegionResults() {
        return outRegionResults;
    }

    public BciAlegroData getBciAlegroData() {
        return bciAlegroData;
    }
}

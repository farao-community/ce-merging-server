package com.farao_community.farao.ce_merging.common.json_api;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface JsonApiData {
    @JsonProperty
    long getId();

    @JsonProperty
    String getType();
}

package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.common.exception.AbstractServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class JsonApiError {
    @JsonProperty
    private String status;
    @JsonProperty
    private String code;
    @JsonProperty
    private String title;
    @JsonProperty
    private String detail;

    private JsonApiError(final AbstractServiceException exception) {
        this.status = exception.getStatus();
        this.code = exception.getCode();
        this.title = exception.getTitle();
        this.detail = exception.getMessage();
    }

    public static JsonApiError fromServiceException(final AbstractServiceException exception) {
        return new JsonApiError(exception);
    }
}

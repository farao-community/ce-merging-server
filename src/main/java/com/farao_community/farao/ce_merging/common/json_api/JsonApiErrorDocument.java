package com.farao_community.farao.ce_merging.common.json_api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class JsonApiErrorDocument {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<JsonApiError> errors;

    public static JsonApiErrorDocument fromErrors(final List<JsonApiError> errors) {
        return new JsonApiErrorDocument(errors);
    }

    public static JsonApiErrorDocument fromError(final Exception e,
                                                 final String status,
                                                 final String title) {
        return new JsonApiErrorDocument(Collections.singletonList(new JsonApiError(status, e.getLocalizedMessage(), title, e.getMessage())));
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.common.exception.AbstractServiceException;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

public final class JsonApiDocument<T extends JsonApiData> {
    @JsonInclude(NON_EMPTY)
    public List<JsonApiError> errors;
    @JsonInclude(NON_NULL)
    public JsonApiMeta meta;
    @JsonInclude(NON_NULL)
    public List<T> data;

    private JsonApiDocument(List<T> data) {
        this.data = data;
    }

    public static <T extends JsonApiData> JsonApiDocument<T> fromDataList(List<T> data) {
        return new JsonApiDocument<>(data);
    }

    public static <T extends JsonApiData> JsonApiDocument<T> fromData(T data) {
        return new JsonApiDocument<>(Collections.singletonList(data));
    }

    private JsonApiDocument(final AbstractServiceException exception) {
        this.errors = Collections.singletonList(JsonApiError.fromServiceException(exception));
        this.data = null;
        this.meta = null;
    }

    public static  <T extends JsonApiData>  JsonApiDocument<T> fromServiceException(final AbstractServiceException exception) {
        return new JsonApiDocument<>(exception);
    }
}

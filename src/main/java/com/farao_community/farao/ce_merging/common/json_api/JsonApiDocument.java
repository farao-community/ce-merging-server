/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.common.exception.AbstractServiceException;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Collections.singletonList;

public final class JsonApiDocument<T extends JsonApiData> {
    @JsonInclude(NON_EMPTY)
    public final List<JsonApiError> errors;
    @JsonInclude(NON_NULL)
    public final List<T> data;

    private JsonApiDocument(final List<T> data) {
        this.data = data;
        this.errors = new ArrayList<>();
    }

    public static <T extends JsonApiData> JsonApiDocument<T> fromDataList(final List<T> data) {
        return new JsonApiDocument<>(data);
    }

    public static <T extends JsonApiData> JsonApiDocument<T> fromData(final T data) {
        return new JsonApiDocument<>(singletonList(data));
    }

    private JsonApiDocument(final AbstractServiceException exception) {
        this.errors = singletonList(JsonApiError.fromServiceException(exception));
        this.data = null;
    }

    public static  <T extends JsonApiData>  JsonApiDocument<T> fromServiceException(final AbstractServiceException exception) {
        return new JsonApiDocument<>(exception);
    }
}

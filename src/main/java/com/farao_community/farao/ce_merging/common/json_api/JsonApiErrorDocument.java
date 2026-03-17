/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.ListUtils.deNulledList;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static java.util.Collections.singletonList;

public class JsonApiErrorDocument {
    @JsonInclude(NON_NULL)
    List<JsonApiError> errors;

    public static JsonApiErrorDocument fromErrors(final List<JsonApiError> errors) {
        return new JsonApiErrorDocument(deNulledList(errors));
    }

    public static JsonApiErrorDocument fromError(final Exception e,
                                                 final String status,
                                                 final String title) {
        return new JsonApiErrorDocument(singletonList(new JsonApiError(status,
                                                                       e.getLocalizedMessage(),
                                                                       title,
                                                                       e.getMessage())));
    }

    public List<JsonApiError> getErrors() {
        return errors;
    }

    public void setErrors(final List<JsonApiError> errors) {
        this.errors = errors;
    }

    public JsonApiErrorDocument(final List<JsonApiError> errors) {
        this.errors = errors;
    }

    public JsonApiErrorDocument() {
        this.errors = new ArrayList<>();
    }
}

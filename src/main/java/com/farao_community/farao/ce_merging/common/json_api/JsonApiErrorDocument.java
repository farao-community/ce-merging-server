/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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

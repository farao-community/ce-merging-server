/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

import com.farao_community.farao.ce_merging.common.exception.AbstractServiceException;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    public JsonApiError(final String status,
                        final String code,
                        final String title,
                        final String detail) {
        this.status = status;
        this.code = code;
        this.title = title;
        this.detail = detail;
    }

    public static JsonApiError fromServiceException(final AbstractServiceException exception) {
        return new JsonApiError(exception);
    }
}

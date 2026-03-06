/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.NONE)
public class CeMergingConstants {
    // HTTP REQUESTS
    public static final String CE_MERGING_URL = "/ce-merging/v1";
    public static final String JSON_API_MIME_TYPE = "application/vnd.api+json";
    public static final String ORIGIN_ANY = "*";
    // SWAGGER
    public static final String MERGING_TASK_ID = "Merging task ID";
    public static final String TASK_NOT_RUN = "Merging task with given ID has not been run";
    public static final String OK = "200";
    public static final String BAD_REQUEST = "400";
    public static final String NOT_FOUND = "404";
    //MISC
    public static final String CSV_SEPARATOR = ";";
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.json_api;

public class JsonApiTestResources {
    public static final String JSON_FROM_EXCEPTION = "{\"errors\":[{\"status\":\"500\",\"code\":\"Test\"," +
                                                     "\"title\":\"TEST\",\"detail\":\"Test\"}]}";

    public static final String JSON_FROM_ERROR = "{\"errors\":[" +
                                                 "{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\",\"title\":\"IO exception\",\"detail\":\"Test\"}," +
                                                 "{\"status\":\"500\",\"code\":\"500-IO-EXCEPTION\",\"title\":\"IO exception\",\"detail\":\"Test\"}" +
                                                 "]}";
}

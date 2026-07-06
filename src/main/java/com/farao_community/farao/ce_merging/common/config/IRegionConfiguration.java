/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public interface IRegionConfiguration {

    String getId();

    void setId(final String id);

    String getName();

    void setName(final String name);

    Map<String, String> getAreasIn();

    @JsonIgnore
    default String getAreaInEic(final String areaCode) {
        return getAreasIn().get(areaCode);
    }

    void setAreasIn(final Map<String, String> areasIn);

    Map<String, String> getAreasOut();

    void setAreasOut(final Map<String, String> areasOut);
}

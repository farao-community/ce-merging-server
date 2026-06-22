/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.json;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.RegionConfigurationDto;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRegionConfiguration {

    private RegionConfigurationDto regionConfiguration;

    @JsonCreator
    public JsonRegionConfiguration(@JsonProperty("regionConfiguration") final RegionConfigurationDto regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }

    public RegionConfigurationDto getRegionConfiguration() {
        return regionConfiguration;
    }

    public void setRegionConfiguration(final RegionConfigurationDto regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }
}

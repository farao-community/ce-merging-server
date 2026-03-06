/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;
import lombok.Setter;

public class JsonRegionConfiguration {

    @Getter
    @Setter
    private RegionConfigurationDto regionConfiguration;

    @JsonCreator
    public JsonRegionConfiguration(RegionConfigurationDto regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }
}

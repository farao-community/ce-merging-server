/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.region_eic;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Data;
@Data
public class JsonRegionConfiguration {

    private RegionConfigurationDto regionConfiguration;

    @JsonCreator
    public JsonRegionConfiguration(RegionConfigurationDto regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }
}

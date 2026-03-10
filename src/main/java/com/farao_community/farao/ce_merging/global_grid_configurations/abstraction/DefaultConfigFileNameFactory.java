/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.abstraction;

import com.farao_community.farao.ce_merging.global_grid_configurations.bilateral_exchanges.JsonBecConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.hvdc_alignment.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.region_eic.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.xnodes.JsonXNodeConfiguration;
import com.powsybl.openrao.virtualhubs.json.JsonVirtualHubsConfiguration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class DefaultConfigFileNameFactory {

    private static final Map<Class<?>, String> DEFAULT_NAMES =
        Map.of(
            JsonBecConfiguration.class, "sharingKeysBEC.csv",
            JsonHvdcAlignmentConfiguration.class, "hvdc-xnode-alignment-configuration.json",
            JsonRegionConfiguration.class, "region_configuration.json",
            JsonVirtualHubsConfiguration.class, "virtual-hubs-default-config.xml",
            JsonXNodeConfiguration.class, "cvg-xnodes-default-config.xml"
        );

    public static <T> String getDefaultConfigFileName(final Class<T> configurationClass) {
        return DEFAULT_NAMES.get(configurationClass);
    }
}

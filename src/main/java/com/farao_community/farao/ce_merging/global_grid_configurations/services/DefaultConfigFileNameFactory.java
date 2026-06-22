/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;

import java.util.Map;

public final class DefaultConfigFileNameFactory {
    private DefaultConfigFileNameFactory() {
        /* This utility class should not be instantiated */
    }

    private static final Map<Class<?>, String> DEFAULT_NAMES =
        Map.of(
            BECKeyConfigurationRecord.class, "sharingKeysBEC.csv",
            HvdcAlignmentConfigurationRecord.class, "hvdc-xnode-alignment-configuration.json",
            RegionConfigurationRecord.class, "region_configuration.json",
            VirtualHubsConfigurationRecord.class, "virtual-hubs-default-config.xml",
            XNodeConfigurationRecord.class, "cvg-xnodes-default-config.xml"
        );

    public static <T> String getDefaultConfigFileName(final Class<T> configurationClass) {
        return DEFAULT_NAMES.get(configurationClass);
    }
}

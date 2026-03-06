/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.virtual_hubs;

import com.farao_community.farao.ce_merging.merging.global_grid_configurations.GridConfigurationRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualHubsConfigurationRepository extends GridConfigurationRepository<VirtualHubsConfigurationRecord> {
}

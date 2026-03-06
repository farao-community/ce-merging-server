/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.bilateral_exchanges;

import com.farao_community.farao.ce_merging.merging.global_grid_configurations.GridConfigurationRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BECKeyConfigurationRepository extends GridConfigurationRepository<BECKeyConfigurationRecord> {
}

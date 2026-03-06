/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.bilateral_exchanges;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JsonBecConfiguration {
    private List<BecByBoundaryDto> becByBoundaries;
}

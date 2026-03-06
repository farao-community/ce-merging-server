/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.global_grid_configurations.hvdc_alignment;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
public class ZeroFlowNode {
    private String xnode;
    private String countryCode;
}

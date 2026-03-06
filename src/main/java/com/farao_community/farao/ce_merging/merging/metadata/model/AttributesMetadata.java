/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.metadata.model;

import com.farao_community.farao.ce_merging.merging.entities.Inputs;
import lombok.Data;

@Data
public class AttributesMetadata {
    private String name;
    private Inputs inputs;
}

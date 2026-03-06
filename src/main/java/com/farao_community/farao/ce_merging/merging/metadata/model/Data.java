package com.farao_community.farao.ce_merging.merging.metadata.model;

import com.farao_community.farao.ce_merging.merging.entities.Configurations;

@lombok.Data
public class Data {
    private String type;
    private AttributesMetadata attributes;
    private Configurations configurations;
}

/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.entities.enums.IgmType;
import lombok.Data;

import java.io.Serializable;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Data
public class IgmDto implements Serializable {
    /**
     * The country of the IGM
     */
    private String country;
    /**
     * The type of the IGM
     */
    private IgmType type;
    /**
     * The location  of the IGM file
     */
    private String igmFileLocation;
    /**
     * The location  of the IGM quality check file
     */
    private String igmQualityReportLocation;
}

/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.entities.enums.IgmType;

import java.io.Serializable;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
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

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public IgmType getType() {
        return type;
    }

    public void setType(IgmType type) {
        this.type = type;
    }

    public String getIgmFileLocation() {
        return igmFileLocation;
    }

    public void setIgmFileLocation(String location) {
        this.igmFileLocation = location;
    }

    public String getIgmQualityReportLocation() {
        return igmQualityReportLocation;
    }

    public void setIgmQualityReportFileLocation(String igmQualityReportFileLocation) {
        this.igmQualityReportLocation = igmQualityReportFileLocation;
    }
}

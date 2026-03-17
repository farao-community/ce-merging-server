/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import com.farao_community.farao.ce_merging.merging.task.entities.enums.IgmType;

import java.io.Serializable;

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

    public String getIgmQualityReportLocation() {
        return igmQualityReportLocation;
    }

    public void setIgmQualityReportLocation(final String igmQualityReportLocation) {
        this.igmQualityReportLocation = igmQualityReportLocation;
    }

    public String getIgmFileLocation() {
        return igmFileLocation;
    }

    public void setIgmFileLocation(final String igmFileLocation) {
        this.igmFileLocation = igmFileLocation;
    }

    public IgmType getType() {
        return type;
    }

    public void setType(final IgmType type) {
        this.type = type;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import com.farao_community.farao.ce_merging.merging.task.enums.IgmType;

import java.io.Serializable;
/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */

public class IgmDto implements Serializable {
    private String country;
    private IgmType type;
    private String igmFileLocation;
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

/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OutputsDto implements Serializable {
    private String refProgLocation;
    private String cgmLocation;
    private String bciReportLocation;

    public String getRefProgLocation() {
        return refProgLocation;
    }

    public void setRefProgLocation(String refProgLocation) {
        this.refProgLocation = refProgLocation;
    }

    public String getCgmLocation() {
        return cgmLocation;
    }

    public void setCgmLocation(String cgmLocation) {
        this.cgmLocation = cgmLocation;
    }

    public String getBciReportLocation() {
        return bciReportLocation;
    }

    public void setBciReportLocation(String bciReportLocation) {
        this.bciReportLocation = bciReportLocation;
    }
}


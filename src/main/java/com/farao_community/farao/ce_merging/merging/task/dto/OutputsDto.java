/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)

public class OutputsDto implements Serializable {
    private String refProgLocation;
    private String cgmLocation;
    private String bciReportLocation;

    public String getBciReportLocation() {
        return bciReportLocation;
    }

    public void setBciReportLocation(final String bciReportLocation) {
        this.bciReportLocation = bciReportLocation;
    }

    public String getCgmLocation() {
        return cgmLocation;
    }

    public void setCgmLocation(final String cgmLocation) {
        this.cgmLocation = cgmLocation;
    }

    public String getRefProgLocation() {
        return refProgLocation;
    }

    public void setRefProgLocation(final String refProgLocation) {
        this.refProgLocation = refProgLocation;
    }
}


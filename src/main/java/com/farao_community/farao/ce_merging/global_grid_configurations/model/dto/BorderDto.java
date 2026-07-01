/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.model.dto;

import jakarta.persistence.Embeddable;

@Embeddable
public class BorderDto {
    private String outArea;
    private String inArea;

    public BorderDto(final String outArea, final String inArea) {
        this.outArea = outArea;
        this.inArea = inArea;
    }

    public BorderDto() {

    }

    public String getOutArea() {
        return outArea;
    }

    public void setOutArea(final String outArea) {
        this.outArea = outArea;
    }

    public String getInArea() {
        return inArea;
    }

    public void setInArea(final String inArea) {
        this.inArea = inArea;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.entities.enums.IgmType;
import lombok.Data;

import java.io.Serializable;

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

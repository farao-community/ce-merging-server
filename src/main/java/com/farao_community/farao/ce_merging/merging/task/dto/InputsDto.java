/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
public class InputsDto implements Serializable {

    private static final int DEFAULT_ALEGRO_THRESHOLD = 2000;

    /**
     * The target Date and Time of the merging process
     */
    private OffsetDateTime targetDate;
    /**
     * mergingWithInternalHvdc = True if there is an internal HVDC line in CE
     * else mergingWithInternalHvdc = False
     */
    private Boolean mergingWithInternalHvdc;
    /**
     * alegroThreshold = 2 GW by default
     * This threshold is used to compare loads between the two xnodes of Alegro
     */
    private Integer alegroThreshold;
    /**
     * The list of IGM data
     */
    private List<IgmDto> igms;
    /**
     * The location of the generation load shift key
     */
    private String generationLoadShiftKeysLocation;
    /**
     * The location of the external constraints
     */
    private String externalConstraintsLocation;
    /**
     * The location of the feasibility ranges
     */
    private String feasibilityRangesLocation;
    /**
     * The location of the dc links
     */
    private String dcLinksLocation;
    /**
     * The location of merging request
     */
    private String mergingRequestLocation;
    /**
     * The location of the net position forecast
     */
    private String netPositionForecastLocation;

    public Integer getAlegroThreshold() {
        return Optional.ofNullable(alegroThreshold).orElse(DEFAULT_ALEGRO_THRESHOLD);
    }

}

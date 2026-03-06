/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Data
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
     * alegroThreshold = 50 MW by default
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
     * The location of the generation load shift key
     */
    private String externalConstraintsLocation;
    /**
     * The location of the generation load shift key
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

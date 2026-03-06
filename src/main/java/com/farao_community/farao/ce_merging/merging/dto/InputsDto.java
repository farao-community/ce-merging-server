/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
public class InputsDto implements Serializable {
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

    public OffsetDateTime getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(OffsetDateTime targetDate) {
        this.targetDate = targetDate;
    }

    public List<IgmDto> getIgms() {
        return igms;
    }

    public void setIgms(List<IgmDto> igms) {
        this.igms = igms;
    }

    public String getGenerationLoadShiftKeysLocation() {
        return generationLoadShiftKeysLocation;
    }

    public void setGenerationLoadShiftKeysLocation(String generationLoadShiftKeysLocation) {
        this.generationLoadShiftKeysLocation = generationLoadShiftKeysLocation;
    }

    public String getExternalConstraintsLocation() {
        return externalConstraintsLocation;
    }

    public void setExternalConstraintsLocation(String externalConstraintsLocation) {
        this.externalConstraintsLocation = externalConstraintsLocation;
    }

    public String getFeasibilityRangesLocation() {
        return feasibilityRangesLocation;
    }

    public void setFeasibilityRangesLocation(String feasibilityRangesLocation) {
        this.feasibilityRangesLocation = feasibilityRangesLocation;
    }

    public String getDcLinksLocation() {
        return dcLinksLocation;
    }

    public void setDcLinksLocation(String dcLinks) {
        this.dcLinksLocation = dcLinks;
    }

    public String getNetPositionForecastLocation() {
        return netPositionForecastLocation;
    }

    public void setNetPositionForecastLocation(String netPositionForecastLocation) {
        this.netPositionForecastLocation = netPositionForecastLocation;
    }

    public String getMergingRequestLocation() {
        return mergingRequestLocation;
    }

    public void setMergingRequestLocation(String mergingRequestLocation) {
        this.mergingRequestLocation = mergingRequestLocation;
    }

    public Boolean isMergingWithInternalHvdc() {
        return mergingWithInternalHvdc;
    }

    public void setMergingWithInternalHvdc(Boolean mergingWithInternalHvdc) {
        this.mergingWithInternalHvdc = mergingWithInternalHvdc;
    }

    public int getAlegroThreshold() {
        if (alegroThreshold != null) {
            return alegroThreshold;
        } else {
            return 2000;
        }
    }

    public void setAlegroThreshold(int alegroThreshold) {
        this.alegroThreshold = alegroThreshold;
    }
}

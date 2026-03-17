/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

@Embeddable
public class Inputs implements Serializable {

    @JsonIgnore
    private static final int DEFAULT_ALEGRO_THRESHOLD = 2000;
    @JsonIgnore
    private static final ZoneOffset DEFAULT_OFFSET = ZoneOffset.of("+01:00");
    /**
     * The target Date and Time of the merging process
     */
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime targetDate;
    /**
     * The real offset of the process target Date
     */
    private ZoneOffset realOffset;
    /**
     * mergingWithInternalHvdc = True if there is an internal HVDC line in CE
     * else mergingWithInternalHvdc = true
     */
    private Boolean mergingWithInternalHvdc;
    /**
     * alegroThreshold = 2 GW by default
     * This threshold is used to compare loads between the two xnodes of Alegro
     */
    private Integer alegroThreshold;
    /**
     * The list of TGM data
     */
    @ElementCollection(fetch = EAGER)
    private List<IgmData> igms = new ArrayList<>();
    /**
     * The original name of the generation load shift key file
     */
    @OneToOne(cascade = ALL)
    private SavedFile generationLoadShiftKeys = new SavedFile();
    /**
     * The SavedFile of the external constraints file
     */
    @OneToOne(cascade = ALL)
    private SavedFile externalConstraints = new SavedFile();
    /**
     * The SavedFile of the feasibility range file
     */
    @OneToOne(cascade = ALL)
    private SavedFile feasibilityRanges = new SavedFile();

    /**
     * The SavedFile of the net position forecast file
     */
    @OneToOne(cascade = ALL)
    private SavedFile netPositionForecast = new SavedFile();

    public IgmData getIgm(final String country) {
        return igms.stream()
            .filter(igmData -> igmData.getCountry().equals(country))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(String.format("Task does not contain country '%s' IGM",
                                                                          country)));
    }

    public ZoneOffset getRealOffset() {
        return Optional.ofNullable(realOffset)
            .orElse(DEFAULT_OFFSET);
    }

    public Boolean isMergingWithInternalHvdc() {
        return Optional.ofNullable(mergingWithInternalHvdc)
            .orElse(true);
    }

    public int getAlegroThreshold() {
        return Optional.ofNullable(alegroThreshold)
            .orElse(DEFAULT_ALEGRO_THRESHOLD);
    }

    // tagged falsely unused in IntelliJ - used for configuration deserialization

    public void setGenerationLoadShiftKeysFilePath(final String filePath) {
        generationLoadShiftKeys.feedPathAndName(filePath);
    }

    public void setFeasibilityRangesFilePath(final String filePath) {
        feasibilityRanges.feedPathAndName(filePath);
    }

    public void setExternalConstraintsFilePath(final String filePath) {
        externalConstraints.feedPathAndName(filePath);
    }

    public void setNetPositionForecastFilePath(final String filePath) {
        netPositionForecast.feedPathAndName(filePath);
    }

    public List<IgmData> getIgms() {
        return igms;
    }

    public void setIgms(final List<IgmData> igms) {
        this.igms = igms;
    }

    public OffsetDateTime getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(final OffsetDateTime targetDate) {
        this.targetDate = targetDate;
    }

    public void setRealOffset(final ZoneOffset realOffset) {
        this.realOffset = realOffset;
    }

    public Boolean getMergingWithInternalHvdc() {
        return mergingWithInternalHvdc;
    }

    public void setMergingWithInternalHvdc(final Boolean mergingWithInternalHvdc) {
        this.mergingWithInternalHvdc = mergingWithInternalHvdc;
    }

    public void setAlegroThreshold(final Integer alegroThreshold) {
        this.alegroThreshold = alegroThreshold;
    }

    public SavedFile getGenerationLoadShiftKeys() {
        return generationLoadShiftKeys;
    }

    public void setGenerationLoadShiftKeys(final SavedFile generationLoadShiftKeys) {
        this.generationLoadShiftKeys = generationLoadShiftKeys;
    }

    public SavedFile getExternalConstraints() {
        return externalConstraints;
    }

    public void setExternalConstraints(final SavedFile externalConstraints) {
        this.externalConstraints = externalConstraints;
    }

    public SavedFile getFeasibilityRanges() {
        return feasibilityRanges;
    }

    public void setFeasibilityRanges(final SavedFile feasibilityRanges) {
        this.feasibilityRanges = feasibilityRanges;
    }

    public SavedFile getNetPositionForecast() {
        return netPositionForecast;
    }

    public void setNetPositionForecast(final SavedFile netPositionForecast) {
        this.netPositionForecast = netPositionForecast;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.exception.task.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.util.OffsetDateTimeSerializer;
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

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DEFAULT_ALEGRO_THRESHOLD;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DEFAULT_REQUEST_OFFSET;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.EAGER;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
@Embeddable
public class Inputs implements Serializable {
    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    private OffsetDateTime targetDate;
    private ZoneOffset realOffset;
    private Boolean mergingWithInternalHvdc;
    /**
     * alegroThreshold = 2 GW by default
     * This threshold is used to compare loads between the two xnodes of Alegro
     */
    private Integer alegroThreshold;
    @ElementCollection(fetch = EAGER)
    private List<IgmData> igms = new ArrayList<>();
    @OneToOne(cascade = ALL)
    private SavedFile generationLoadShiftKeys = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile externalConstraints = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile feasibilityRanges = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile netPositionForecast = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile dcLinks = new SavedFile();

    public IgmData getIgm(final String country) {
        return igms.stream()
            .filter(igmData -> igmData.getCountry().equals(country))
            .findAny()
            .orElseThrow(() -> new TaskNotValidException(String.format("Task does not contain country '%s' IGM",
                                                                       country)));
    }

    public ZoneOffset getRealOffset() {
        return Optional.ofNullable(realOffset)
            .orElse(DEFAULT_REQUEST_OFFSET);
    }

    public Boolean getMergingWithInternalHvdc() {
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

    public void setDcLinksFilePath(final String filePath) {
        dcLinks.feedPathAndName(filePath);
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

    public SavedFile getDcLinks() {
        return dcLinks;
    }

    public void setDcLinks(final SavedFile dcLinks) {
        this.dcLinks = dcLinks;
    }
}

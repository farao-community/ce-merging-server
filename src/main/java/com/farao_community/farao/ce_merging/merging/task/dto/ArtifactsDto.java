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
public class ArtifactsDto implements Serializable {
    private String germanPreMergedIgmFileLocation;
    private String dkConvertedFileLocation;
    private String topologicalMergeFileLocation;
    private String glskQualityReportLocation;
    private String igmsNetPositionsFileLocation;
    private String tgmNetPositionsFileLocation;
    private String bciOutputFileLocation;
    private String balancesAdjustmentTargetFileLocation;
    private String cgmNetPositionsFileLocation;
    private String pstOutputFileLocation;
    private String balancedCgmFileLocation;
    private String pstAppliedCgmFileLocation;
    private String executionLogsForMergingSupervisorLocation;
    private String loadflowOnFinalCgmLogsLocation;
    private String alegroNetPositionsLocation;

    public String getDkConvertedFileLocation() {
        return dkConvertedFileLocation;
    }

    public void setDkConvertedFileLocation(final String dkConvertedFileLocation) {
        this.dkConvertedFileLocation = dkConvertedFileLocation;
    }

    public String getGermanPreMergedIgmFileLocation() {
        return germanPreMergedIgmFileLocation;
    }

    public void setGermanPreMergedIgmFileLocation(final String germanPreMergedIgmFileLocation) {
        this.germanPreMergedIgmFileLocation = germanPreMergedIgmFileLocation;
    }

    public String getTopologicalMergeFileLocation() {
        return topologicalMergeFileLocation;
    }

    public void setTopologicalMergeFileLocation(final String topologicalMergeFileLocation) {
        this.topologicalMergeFileLocation = topologicalMergeFileLocation;
    }

    public String getGlskQualityReportLocation() {
        return glskQualityReportLocation;
    }

    public void setGlskQualityReportLocation(final String glskQualityReportLocation) {
        this.glskQualityReportLocation = glskQualityReportLocation;
    }

    public String getIgmsNetPositionsFileLocation() {
        return igmsNetPositionsFileLocation;
    }

    public void setIgmsNetPositionsFileLocation(final String igmsNetPositionsFileLocation) {
        this.igmsNetPositionsFileLocation = igmsNetPositionsFileLocation;
    }

    public String getTgmNetPositionsFileLocation() {
        return tgmNetPositionsFileLocation;
    }

    public void setTgmNetPositionsFileLocation(final String tgmNetPositionsFileLocation) {
        this.tgmNetPositionsFileLocation = tgmNetPositionsFileLocation;
    }

    public String getBciOutputFileLocation() {
        return bciOutputFileLocation;
    }

    public void setBciOutputFileLocation(final String bciOutputFileLocation) {
        this.bciOutputFileLocation = bciOutputFileLocation;
    }

    public String getBalancesAdjustmentTargetFileLocation() {
        return balancesAdjustmentTargetFileLocation;
    }

    public void setBalancesAdjustmentTargetFileLocation(final String balancesAdjustmentTargetFileLocation) {
        this.balancesAdjustmentTargetFileLocation = balancesAdjustmentTargetFileLocation;
    }

    public String getCgmNetPositionsFileLocation() {
        return cgmNetPositionsFileLocation;
    }

    public void setCgmNetPositionsFileLocation(final String cgmNetPositionsFileLocation) {
        this.cgmNetPositionsFileLocation = cgmNetPositionsFileLocation;
    }

    public String getPstOutputFileLocation() {
        return pstOutputFileLocation;
    }

    public void setPstOutputFileLocation(final String pstOutputFileLocation) {
        this.pstOutputFileLocation = pstOutputFileLocation;
    }

    public String getBalancedCgmFileLocation() {
        return balancedCgmFileLocation;
    }

    public void setBalancedCgmFileLocation(final String balancedCgmFileLocation) {
        this.balancedCgmFileLocation = balancedCgmFileLocation;
    }

    public String getPstAppliedCgmFileLocation() {
        return pstAppliedCgmFileLocation;
    }

    public void setPstAppliedCgmFileLocation(final String pstAppliedCgmFileLocation) {
        this.pstAppliedCgmFileLocation = pstAppliedCgmFileLocation;
    }

    public String getExecutionLogsForMergingSupervisorLocation() {
        return executionLogsForMergingSupervisorLocation;
    }

    public void setExecutionLogsForMergingSupervisorLocation(final String executionLogsForMergingSupervisorLocation) {
        this.executionLogsForMergingSupervisorLocation = executionLogsForMergingSupervisorLocation;
    }

    public String getLoadflowOnFinalCgmLogsLocation() {
        return loadflowOnFinalCgmLogsLocation;
    }

    public void setLoadflowOnFinalCgmLogsLocation(final String loadflowOnFinalCgmLogsLocation) {
        this.loadflowOnFinalCgmLogsLocation = loadflowOnFinalCgmLogsLocation;
    }

    public String getAlegroNetPositionsLocation() {
        return alegroNetPositionsLocation;
    }

    public void setAlegroNetPositionsLocation(final String alegroNetPositionsLocation) {
        this.alegroNetPositionsLocation = alegroNetPositionsLocation;
    }
}

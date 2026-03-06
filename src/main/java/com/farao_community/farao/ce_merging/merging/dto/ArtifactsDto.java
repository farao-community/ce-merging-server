/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * WARNING: this class is used by the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    public String getGermanPreMergedIgmFileLocation() {
        return germanPreMergedIgmFileLocation;
    }

    public void setGermanPreMergedIgmFileLocation(String germanPreMergedIgmFileLocation) {
        this.germanPreMergedIgmFileLocation = germanPreMergedIgmFileLocation;
    }

    public String getDkConvertedFileLocation() {
        return dkConvertedFileLocation;
    }

    public void setDkConvertedFileLocation(String dkConvertedFileLocation) {
        this.dkConvertedFileLocation = dkConvertedFileLocation;
    }

    public String getTopologicalMergeFileLocation() {
        return topologicalMergeFileLocation;
    }

    public void setTopologicalMergeFileLocation(String topologicalMergeFileLocation) {
        this.topologicalMergeFileLocation = topologicalMergeFileLocation;
    }

    public String getGlskQualityReportLocation() {
        return glskQualityReportLocation;
    }

    public void setGlskQualityReportLocation(String glskQualityReportLocation) {
        this.glskQualityReportLocation = glskQualityReportLocation;
    }

    public String getIgmsNetPositionsFileLocation() {
        return igmsNetPositionsFileLocation;
    }

    public void setIgmsNetPositionsFileLocation(String igmsNetPositionsFileLocation) {
        this.igmsNetPositionsFileLocation = igmsNetPositionsFileLocation;
    }

    public String getBciOutputFileLocation() {
        return bciOutputFileLocation;
    }

    public void setBciOutputFileLocation(String bciOutputFileLocation) {
        this.bciOutputFileLocation = bciOutputFileLocation;
    }

    public String getBalancesAdjustmentTargetFileLocation() {
        return balancesAdjustmentTargetFileLocation;
    }

    public void setBalancesAdjustmentTargetFileLocation(String balancesAdjustmentTargetFileLocation) {
        this.balancesAdjustmentTargetFileLocation = balancesAdjustmentTargetFileLocation;
    }

    public String getCgmNetPositionsFileLocation() {
        return cgmNetPositionsFileLocation;
    }

    public void setCgmNetPositionsFileLocation(String cgmNetPositionsFileLocation) {
        this.cgmNetPositionsFileLocation = cgmNetPositionsFileLocation;
    }

    public String getTgmNetPositionsFileLocation() {
        return tgmNetPositionsFileLocation;
    }

    public void setTgmNetPositionsFileLocation(String tgmNetPositionsFileLocation) {
        this.tgmNetPositionsFileLocation = tgmNetPositionsFileLocation;
    }

    public String getPstOutputFileLocation() {
        return pstOutputFileLocation;
    }

    public void setPstOutputFileLocation(String pstOutputFileLocation) {
        this.pstOutputFileLocation = pstOutputFileLocation;
    }

    public String getBalancedCgmFileLocation() {
        return balancedCgmFileLocation;
    }

    public void setBalancedCgmFileLocation(String balancedCgmFileLocation) {
        this.balancedCgmFileLocation = balancedCgmFileLocation;
    }

    public String getPstAppliedCgmFileLocation() {
        return pstAppliedCgmFileLocation;
    }

    public void setPstAppliedCgmFileLocation(String pstAppliedCgmFileLocation) {
        this.pstAppliedCgmFileLocation = pstAppliedCgmFileLocation;
    }

    public String getExecutionLogsForMergingSupervisorLocation() {
        return executionLogsForMergingSupervisorLocation;
    }

    public void setExecutionLogsForMergingSupervisorLocation(String executionLogsForMergingSupervisorLocation) {
        this.executionLogsForMergingSupervisorLocation = executionLogsForMergingSupervisorLocation;
    }

    public String getLoadflowOnFinalCgmLogsLocation() {
        return loadflowOnFinalCgmLogsLocation;
    }

    public void setLoadflowOnFinalCgmLogsLocation(String loadflowOnFinalCgmLogsLocation) {
        this.loadflowOnFinalCgmLogsLocation = loadflowOnFinalCgmLogsLocation;
    }

    public String getAlegroNetPositionsLocation() {
        return alegroNetPositionsLocation;
    }

    public void setAlegroNetPositionsLocation(String alegroNetPositionsLocation) {
        this.alegroNetPositionsLocation = alegroNetPositionsLocation;
    }
}

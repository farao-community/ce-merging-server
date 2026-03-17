/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

@Embeddable
public class Artifacts implements Serializable {
    @OneToOne(cascade = ALL)
    private SavedFile germanPreMergedIgmFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile dkConvertedFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile topologicalMergeFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile balancedCgmFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile cgmFileAfterPst = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile glskQualityReport = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile glskQualityCorrectedFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile igmsNetPositionsFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile germanIgmsNetPositionsFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile bciOutputFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile balancesAdjustmentTargetFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile cgmNetPositionsFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile tgmFileAfterRecessivity = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile tgmNetPositionsFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile pstOutputFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile mergingLogsExportedFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile executionLogsForMergingSupervisor = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile loadflowOnFinalCgmLogs = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile alegroNetPositions = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile xnodesInformationFile = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile xnodesInconsistencies = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile referenceProgramForecastFile = new SavedFile();

    @Transient
    private Map<String, SavedFile> preTreatedIgmMap = new HashMap<>();

    public SavedFile getGermanPreMergedIgmFile() {
        return germanPreMergedIgmFile;
    }

    public void setGermanPreMergedIgmFile(final SavedFile germanPreMergedIgmFile) {
        this.germanPreMergedIgmFile = germanPreMergedIgmFile;
    }

    public SavedFile getDkConvertedFile() {
        return dkConvertedFile;
    }

    public void setDkConvertedFile(final SavedFile dkConvertedFile) {
        this.dkConvertedFile = dkConvertedFile;
    }

    public SavedFile getTopologicalMergeFile() {
        return topologicalMergeFile;
    }

    public void setTopologicalMergeFile(final SavedFile topologicalMergeFile) {
        this.topologicalMergeFile = topologicalMergeFile;
    }

    public SavedFile getBalancedCgmFile() {
        return balancedCgmFile;
    }

    public void setBalancedCgmFile(final SavedFile balancedCgmFile) {
        this.balancedCgmFile = balancedCgmFile;
    }

    public SavedFile getCgmFileAfterPst() {
        return cgmFileAfterPst;
    }

    public void setCgmFileAfterPst(final SavedFile cgmFileAfterPst) {
        this.cgmFileAfterPst = cgmFileAfterPst;
    }

    public SavedFile getGlskQualityReport() {
        return glskQualityReport;
    }

    public void setGlskQualityReport(final SavedFile glskQualityReport) {
        this.glskQualityReport = glskQualityReport;
    }

    public SavedFile getGlskQualityCorrectedFile() {
        return glskQualityCorrectedFile;
    }

    public void setGlskQualityCorrectedFile(final SavedFile glskQualityCorrectedFile) {
        this.glskQualityCorrectedFile = glskQualityCorrectedFile;
    }

    public SavedFile getIgmsNetPositionsFile() {
        return igmsNetPositionsFile;
    }

    public void setIgmsNetPositionsFile(final SavedFile igmsNetPositionsFile) {
        this.igmsNetPositionsFile = igmsNetPositionsFile;
    }

    public SavedFile getGermanIgmsNetPositionsFile() {
        return germanIgmsNetPositionsFile;
    }

    public void setGermanIgmsNetPositionsFile(final SavedFile germanIgmsNetPositionsFile) {
        this.germanIgmsNetPositionsFile = germanIgmsNetPositionsFile;
    }

    public SavedFile getBciOutputFile() {
        return bciOutputFile;
    }

    public void setBciOutputFile(final SavedFile bciOutputFile) {
        this.bciOutputFile = bciOutputFile;
    }

    public SavedFile getBalancesAdjustmentTargetFile() {
        return balancesAdjustmentTargetFile;
    }

    public void setBalancesAdjustmentTargetFile(final SavedFile balancesAdjustmentTargetFile) {
        this.balancesAdjustmentTargetFile = balancesAdjustmentTargetFile;
    }

    public SavedFile getCgmNetPositionsFile() {
        return cgmNetPositionsFile;
    }

    public void setCgmNetPositionsFile(final SavedFile cgmNetPositionsFile) {
        this.cgmNetPositionsFile = cgmNetPositionsFile;
    }

    public SavedFile getTgmFileAfterRecessivity() {
        return tgmFileAfterRecessivity;
    }

    public void setTgmFileAfterRecessivity(final SavedFile tgmFileAfterRecessivity) {
        this.tgmFileAfterRecessivity = tgmFileAfterRecessivity;
    }

    public SavedFile getTgmNetPositionsFile() {
        return tgmNetPositionsFile;
    }

    public void setTgmNetPositionsFile(final SavedFile tgmNetPositionsFile) {
        this.tgmNetPositionsFile = tgmNetPositionsFile;
    }

    public SavedFile getPstOutputFile() {
        return pstOutputFile;
    }

    public void setPstOutputFile(final SavedFile pstOutputFile) {
        this.pstOutputFile = pstOutputFile;
    }

    public SavedFile getMergingLogsExportedFile() {
        return mergingLogsExportedFile;
    }

    public void setMergingLogsExportedFile(final SavedFile mergingLogsExportedFile) {
        this.mergingLogsExportedFile = mergingLogsExportedFile;
    }

    public SavedFile getExecutionLogsForMergingSupervisor() {
        return executionLogsForMergingSupervisor;
    }

    public void setExecutionLogsForMergingSupervisor(final SavedFile executionLogsForMergingSupervisor) {
        this.executionLogsForMergingSupervisor = executionLogsForMergingSupervisor;
    }

    public SavedFile getLoadflowOnFinalCgmLogs() {
        return loadflowOnFinalCgmLogs;
    }

    public void setLoadflowOnFinalCgmLogs(final SavedFile loadflowOnFinalCgmLogs) {
        this.loadflowOnFinalCgmLogs = loadflowOnFinalCgmLogs;
    }

    public SavedFile getAlegroNetPositions() {
        return alegroNetPositions;
    }

    public void setAlegroNetPositions(final SavedFile alegroNetPositions) {
        this.alegroNetPositions = alegroNetPositions;
    }

    public SavedFile getXnodesInformationFile() {
        return xnodesInformationFile;
    }

    public void setXnodesInformationFile(final SavedFile xnodesInformationFile) {
        this.xnodesInformationFile = xnodesInformationFile;
    }

    public SavedFile getXnodesInconsistencies() {
        return xnodesInconsistencies;
    }

    public void setXnodesInconsistencies(final SavedFile xnodesInconsistencies) {
        this.xnodesInconsistencies = xnodesInconsistencies;
    }

    public SavedFile getReferenceProgramForecastFile() {
        return referenceProgramForecastFile;
    }

    public void setReferenceProgramForecastFile(final SavedFile referenceProgramForecastFile) {
        this.referenceProgramForecastFile = referenceProgramForecastFile;
    }

    public Map<String, SavedFile> getPreTreatedIgmMap() {
        return preTreatedIgmMap;
    }

    public void setPreTreatedIgmMap(final Map<String, SavedFile> preTreatedIgmMap) {
        this.preTreatedIgmMap = preTreatedIgmMap;
    }
}

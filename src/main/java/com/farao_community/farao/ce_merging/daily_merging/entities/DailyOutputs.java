/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.entities;

import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;

import java.io.Serializable;

@Embeddable
public class DailyOutputs implements Serializable {

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile refProg = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile mergingResponse = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile mergingLogs = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile glskQualityReport = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile cgmZip = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile xnodesZip = new SavedFile();

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile mergingReport = new SavedFile();

    public SavedFile getRefProg() {
        return refProg;
    }

    public void setRefProg(SavedFile refProg) {
        this.refProg = refProg;
    }

    public SavedFile getMergingResponse() {
        return mergingResponse;
    }

    public void setMergingResponse(SavedFile mergingResponse) {
        this.mergingResponse = mergingResponse;
    }

    public SavedFile getMergingLogs() {
        return mergingLogs;
    }

    public void setMergingLogs(SavedFile mergingLogs) {
        this.mergingLogs = mergingLogs;
    }

    public SavedFile getGlskQualityReport() {
        return glskQualityReport;
    }

    public void setGlskQualityReport(SavedFile glskQualityReport) {
        this.glskQualityReport = glskQualityReport;
    }

    public SavedFile getCgmZip() {
        return cgmZip;
    }

    public void setCgmZip(SavedFile cgmZip) {
        this.cgmZip = cgmZip;
    }

    public SavedFile getXnodesZip() {
        return xnodesZip;
    }

    public void setXnodesZip(SavedFile xnodesZip) {
        this.xnodesZip = xnodesZip;
    }

    public SavedFile getMergingReport() {
        return mergingReport;
    }

    public void setMergingReport(SavedFile mergingReport) {
        this.mergingReport = mergingReport;
    }
}

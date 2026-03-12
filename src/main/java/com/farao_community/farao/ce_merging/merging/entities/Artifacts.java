/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

@Data
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
}

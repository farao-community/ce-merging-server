/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_aggregation.entities;

import com.farao_community.farao.ce_merging.merging.entities.SavedFile;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.io.Serializable;

import static jakarta.persistence.CascadeType.ALL;

@Embeddable
@Data
public class DailyOutputs implements Serializable {

    @OneToOne(cascade = ALL)
    private SavedFile refProg = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile mergingResponse = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile mergingLogs = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile glskQualityReport = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile cgmZip = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile xnodesZip = new SavedFile();

    @OneToOne(cascade = ALL)
    private SavedFile mergingReport = new SavedFile();
}

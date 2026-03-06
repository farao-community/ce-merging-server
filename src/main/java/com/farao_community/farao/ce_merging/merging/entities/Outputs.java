/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

/**
 * WARNING: this class is linked to the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Embeddable
@Data
public class Outputs implements Serializable {
    @OneToOne(cascade = ALL)
    private SavedFile refProg = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile cgm = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile bciReport = new SavedFile();
    @OneToOne(cascade = ALL)
    private SavedFile realGlsk = new SavedFile();
    @OneToMany(cascade = ALL)
    private Map<String, SavedFile> igmQualityChecks = new HashMap<>();
    @OneToOne(cascade = ALL)
    private SavedFile mergingLogs = new SavedFile();

}


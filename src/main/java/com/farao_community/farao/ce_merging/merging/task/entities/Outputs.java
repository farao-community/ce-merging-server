/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
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


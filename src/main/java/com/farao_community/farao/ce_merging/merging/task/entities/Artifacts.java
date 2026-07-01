/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import static jakarta.persistence.CascadeType.ALL;

@Embeddable
public class Artifacts implements Serializable {

    @OneToMany(cascade = ALL)
    private Map<ArtifactType, SavedFile> artifactFiles = new EnumMap<>(ArtifactType.class);

    @Transient
    private Map<String, SavedFile> preTreatedIgmMap = new HashMap<>();

    public SavedFile getFile(final ArtifactType artifactType) {
        return artifactFiles.get(artifactType);
    }

    public void putFile(final ArtifactType type, final SavedFile artifactFile) {
        artifactFiles.put(type, artifactFile);
    }

    public Map<String, SavedFile> getPreTreatedIgmMap() {
        return preTreatedIgmMap;
    }

    public void setPreTreatedIgmMap(Map<String, SavedFile> preTreatedIgmMap) {
        this.preTreatedIgmMap = preTreatedIgmMap;
    }

}

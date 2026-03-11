/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.file.Paths;

@Entity
@Data
@NoArgsConstructor
public class SavedFile implements Serializable {
    @Id
    @GeneratedValue
    private long fileId;

    private String originalName;
    private String path;
    private String location;

    public SavedFile(final String originalName,
                     final String path,
                     final String location) {
        this.originalName = originalName;
        this.path = path;
        this.location = location;
    }

    public void feedPathAndName(final String fullFilePath) {
        this.path = fullFilePath;
        this.originalName = Paths.get(fullFilePath).getFileName().toString();
    }
}

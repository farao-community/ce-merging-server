/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.file.Paths;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SavedFile implements Serializable {
    @Id
    @GeneratedValue
    private long fileId;

    private String originalName;
    private String path;
    private String location;

    public void feedPathAndName(final String fullFilePath) {
        this.path = fullFilePath;
        this.originalName = Paths.get(fullFilePath).getFileName().toString();
    }
}

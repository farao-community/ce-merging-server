/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

@Entity
public class SavedFile implements Serializable {

    @Id
    @GeneratedValue
    private long fileId;

    private String originalName;
    private String path;
    private String location;

    public SavedFile() {
    }

    public SavedFile(String originalName, String path, String location) {
        this.originalName = originalName;
        this.path = path;
        this.location = location;
    }

    public void feedPathAndName(final String fullFilePath) {
        if (StringUtils.isEmpty(fullFilePath)) {
            throw new ServiceIOException("null or empty is not a path");
        }
        this.path = fullFilePath;
        this.originalName = Paths.get(fullFilePath).getFileName().toString();
    }

    public void feedPathAndName(final Path filePath) {
        if (filePath == null) {
            throw new ServiceIOException("null is not a path");
        }
        this.path = filePath.toString();
        this.originalName = filePath.getFileName().toString();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(final String originalName) {
        this.originalName = originalName;
    }

    public long getFileId() {
        return fileId;
    }

    public void setFileId(final long fileId) {
        this.fileId = fileId;
    }
}

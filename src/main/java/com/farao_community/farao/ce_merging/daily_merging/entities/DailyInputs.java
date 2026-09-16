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
import java.nio.file.Paths;

@Embeddable
public class DailyInputs implements Serializable {

    @OneToOne(cascade = CascadeType.ALL)
    private SavedFile mergingRequest = new SavedFile();

    public SavedFile getMergingRequest() {
        return mergingRequest;
    }

    public String getMergingRequestLocation() {
        return mergingRequest.getLocation();
    }

    public void setMergingRequest(SavedFile mergingRequest) {
        this.mergingRequest = mergingRequest;
    }

    public void setMergingRequestFilePath(String mergingRequestFilePath) {
        String filename = Paths.get(mergingRequestFilePath).getFileName().toString();
        mergingRequest.setPath(mergingRequestFilePath);
        mergingRequest.setOriginalName(filename);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data.task;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class BciOutput implements Serializable {

    private String outputFilePath;

    public BciOutput() {
    }

    public BciOutput(final String filePath) {
        this.outputFilePath = filePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }
}

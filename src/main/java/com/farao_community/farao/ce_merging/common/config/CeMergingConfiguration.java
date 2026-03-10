/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

import static java.io.File.separator;

@Configuration
public class CeMergingConfiguration {
    private static final String INPUTS_DIRECTORY = "inputs";
    private static final String OUTPUTS_DIRECTORY = "outputs";
    private static final String DAILY_OUTPUTS_DIRECTORY = "daily-outputs";
    private static final String DAILY_INPUTS_DIRECTORY = "daily-inputs";
    private static final String ARTIFACTS_DIRECTORY = "artifacts";

    @Value("${ce-merging-server.filesystem.root}")
    private String ceMergingRoot;

    @Value("${ce-merging-server.filesystem.root-daily}")
    private String dailyMergingRoot;

    public Path getDirectoryPath(final MergingTask task) {
        return Path.of(ceMergingRoot + separator + task.getTaskId());
    }

    public String getOutputsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task).resolve(OUTPUTS_DIRECTORY).toString();
    }

    public String getInputsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task).resolve(INPUTS_DIRECTORY).toString();
    }

    public String getArtifactsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task).resolve(ARTIFACTS_DIRECTORY).toString();
    }
}

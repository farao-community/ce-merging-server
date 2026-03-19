/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

import static java.io.File.separator;

@Configuration
public class CeMergingConfiguration {
    private static final String INPUTS = "inputs";
    private static final String OUTPUTS = "outputs";
    private static final String DAILY_OUTPUTS = "daily-outputs";
    private static final String DAILY_INPUTS = "daily-inputs";
    private static final String ARTIFACTS = "artifacts";

    @Value("${ce-merging-server.filesystem.root}")
    private String ceMergingRoot;

    @Value("${ce-merging-server.filesystem.root-daily}")
    private String dailyMergingRoot;

    /**
     *
     * @param task : each task has it own directory
     * @param directory : each type of file (in, out, ...) has its subdirectory inside
     * @return /path/to/root/task_id/directory
     */
    private String getDirectoryPath(final MergingTask task, final String directory) {
        return Path.of(ceMergingRoot + separator + task.getTaskId())
            .resolve(directory)
            .toString();
    }

    /**
     *
     * @param task : each task has it own directory
     * @param directory : each type of file (in, out, ...) has its subdirectory inside
     * @return /path/to/root/task_id/directory
     */
    private String getDailyDirectoryPath(final MergingTask task, final String directory) {
        return Path.of(dailyMergingRoot + separator + task.getTaskId())
            .resolve(directory)
            .toString();
    }

    public String getInputsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task, INPUTS);
    }

    public String getDailyInputsDirectoryPath(final MergingTask task) {
        return getDailyDirectoryPath(task, DAILY_INPUTS);
    }

    public String getDailyOutputsDirectoryPath(final MergingTask task) {
        return getDailyDirectoryPath(task, DAILY_OUTPUTS);
    }

    public String getOutputsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task, OUTPUTS);
    }

    public String getArtifactsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task, ARTIFACTS);
    }

    public String getCeMergingRoot() {
        return ceMergingRoot;
    }

    public void setCeMergingRoot(final String ceMergingRoot) {
        this.ceMergingRoot = ceMergingRoot;
    }

    public String getDailyMergingRoot() {
        return dailyMergingRoot;
    }

    public void setDailyMergingRoot(final String dailyMergingRoot) {
        this.dailyMergingRoot = dailyMergingRoot;
    }
}

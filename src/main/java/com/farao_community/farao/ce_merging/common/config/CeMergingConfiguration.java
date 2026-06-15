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

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ARTIFACTS_DIR;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DAILY_INPUTS_DIR;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DAILY_OUTPUTS_DIR;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.INPUTS_DIR;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.OUTPUTS_DIR;
import static java.io.File.separator;

@Configuration
public class CeMergingConfiguration {

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
        return getDirectoryPath(task, INPUTS_DIR);
    }

    public String getDailyInputsDirectoryPath(final MergingTask task) {
        return getDailyDirectoryPath(task, DAILY_INPUTS_DIR);
    }

    public String getDailyOutputsDirectoryPath(final MergingTask task) {
        return getDailyDirectoryPath(task, DAILY_OUTPUTS_DIR);
    }

    public String getOutputsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task, OUTPUTS_DIR);
    }

    public String getArtifactsDirectoryPath(final MergingTask task) {
        return getDirectoryPath(task, ARTIFACTS_DIR);
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

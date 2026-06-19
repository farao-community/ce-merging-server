/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import com.farao_community.farao.ce_merging.common.json_api.JsonApiData;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
public class MergingTaskDto implements JsonApiData {

    private static final String TYPE = "merging-task";

    private Long taskId;
    private String taskName;
    private TaskStatus taskStatus;
    private InputsDto inputs;
    private ConfigurationsDto configurations;
    private OutputsDto outputs;
    private ArtifactsDto artifacts;

    @Override
    public Long getId() {
        return this.taskId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(final TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(final Long taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(final String taskName) {
        this.taskName = taskName;
    }

    public InputsDto getInputs() {
        return inputs;
    }

    public void setInputs(final InputsDto inputs) {
        this.inputs = inputs;
    }

    public ConfigurationsDto getConfigurations() {
        return configurations;
    }

    public void setConfigurations(final ConfigurationsDto configurations) {
        this.configurations = configurations;
    }

    public OutputsDto getOutputs() {
        return outputs;
    }

    public void setOutputs(final OutputsDto outputs) {
        this.outputs = outputs;
    }

    public ArtifactsDto getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(final ArtifactsDto artifacts) {
        this.artifacts = artifacts;
    }
}

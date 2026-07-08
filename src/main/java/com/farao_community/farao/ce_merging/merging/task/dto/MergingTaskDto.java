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

    private Long id;
    private String name;
    private TaskStatus status;
    private InputsDto inputs;
    private ConfigurationsDto configurations;
    private OutputsDto outputs;
    private ArtifactsDto artifacts;

    @Override
    public Long getId() {
        return this.id;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
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

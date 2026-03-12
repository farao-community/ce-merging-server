/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.dto;

import com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiData;
import lombok.Data;

@Data
public class MergingTaskDto implements JsonApiData {

    private long taskId;
    /**
     * The name of the merging Task
     */
    private String name;

    /**
     * The Status of the merging Task
     */
    private TaskStatus taskStatus;

    /**
     * The inputs of the merging Task
     */
    private InputsDto inputs;
    /**
     * The configurations of the merging Task
     */
    private ConfigurationsDto configurations;

    /**
     * the Outputs of the merging task
     */
    private OutputsDto outputs;

    /**
     * the Artifacts of the merging task
     */
    private ArtifactsDto artifacts;

    @Override
    public long getId() {
        return this.taskId;
    }

    @Override
    public String getType() {
        return this.name;
    }
}

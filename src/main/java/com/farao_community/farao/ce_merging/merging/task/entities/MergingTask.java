/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.io.Serializable;

import static jakarta.persistence.GenerationType.AUTO;

@Entity
public class MergingTask implements Serializable {
    @Id
    @GeneratedValue(strategy = AUTO)
    private long taskId;

    /**
     * The name of the merging Task
     */
    private String name;

    /**
     * The Status of the merging Task
     */
    private TaskStatus taskStatus = TaskStatus.CREATED;

    /**
     * Details about task status
     */
    @Column(columnDefinition = "LONGTEXT")
    private String statusDetail;

    /**
     * Http unique traceId filled when a merging request is run used to track logs
     */
    private String runTraceId;

    /**
     * The original name of the .zip input file
     */
    private String archiveFileOriginalName;

    /**
     * The inputs of the merging Task
     */
    @Embedded
    private Inputs inputs = new Inputs();

    /**
     * The artefacts of the merging Task computation
     */
    @Embedded
    private Artifacts artifacts = new Artifacts();

    /**
     * The configurations of the merging Task
     */
    @Embedded
    private Configurations configurations = new Configurations();

    /**
     * the Outputs of the merging task
     */
    @Embedded
    private Outputs outputs = new Outputs();

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(final TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(final long taskId) {
        this.taskId = taskId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(final String statusDetail) {
        this.statusDetail = statusDetail;
    }

    public String getArchiveFileOriginalName() {
        return archiveFileOriginalName;
    }

    public void setArchiveFileOriginalName(final String archiveFileOriginalName) {
        this.archiveFileOriginalName = archiveFileOriginalName;
    }

    public Inputs getInputs() {
        return inputs;
    }

    public void setInputs(final Inputs inputs) {
        this.inputs = inputs;
    }

    public Artifacts getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(final Artifacts artifacts) {
        this.artifacts = artifacts;
    }

    public Configurations getConfigurations() {
        return configurations;
    }

    public void setConfigurations(final Configurations configurations) {
        this.configurations = configurations;
    }

    public Outputs getOutputs() {
        return outputs;
    }

    public void setOutputs(final Outputs outputs) {
        this.outputs = outputs;
    }

}

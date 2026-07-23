/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import com.powsybl.iidm.network.Network;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.apache.commons.io.FilenameUtils;

import java.io.FileNotFoundException;
import java.io.Serializable;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.AUTO;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
@Entity
public class MergingTask implements Serializable {
    @Id
    @GeneratedValue(strategy = AUTO)
    private Long id;
    private String name;
    @Enumerated(STRING)
    private TaskStatus status = TaskStatus.CREATED;
    @Column(columnDefinition = "LONGTEXT")
    private String statusDetail;
    private String archiveFileOriginalName;
    @Embedded
    private Inputs inputs = new Inputs();
    @Embedded
    private Artifacts artifacts = new Artifacts();
    @Embedded
    private Configurations configurations = new Configurations();
    @Embedded
    private Outputs outputs = new Outputs();

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(final TaskStatus taskStatus) {
        this.status = taskStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public <T> T getArtifact(final ArtifactType artifactType, final Class<T> clazz) throws FileNotFoundException {
        final String format = FilenameUtils.getExtension(artifactType.getFileName()).toUpperCase();
        final String path = artifacts.getFile(artifactType).getPath();
        return switch (format){
            case "JSON" -> JsonUtils.read(clazz, path);
            case "XML" -> JaxbUtils.readFromPath(clazz, path);
            case "UCT", "XIIDM" -> clazz == Network.class ? (T) Network.read(path) : null; // NOSONAR this is a Network
            default -> null; // should never happen
        };
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

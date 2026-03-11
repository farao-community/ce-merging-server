/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.entities;

import com.farao_community.farao.ce_merging.merging.entities.enums.TaskStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;

import static jakarta.persistence.GenerationType.AUTO;


@Entity
@Data
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
}

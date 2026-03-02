/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.entities;

import com.farao_community.farao.ce_merging.common.entities.enums.TaskStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.AUTO;

/**
 * WARNING: this class is linked to the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Entity
@Data
public class DailyCeMergingEntity implements Serializable {
    /**
     * Generated Id of daily merging operation
     */
    @Id
    @GeneratedValue(strategy = AUTO)
    private long dailyMergingTaskId;

    /**
     * version of daily merging
     */
    private int version;

    /**
     * The Status of the Core merging Task
     */
    private TaskStatus taskStatus = TaskStatus.CREATED;

    /**
     * List of tasks composing a daily merging operation
     */
    @ElementCollection(fetch = EAGER)
    private List<Long> ceMergingTaskIdsList = new ArrayList<>();

    /**
     * the Outputs of the merging day
     */
    @Embedded
    private DailyOutputs dailyOutputs = new DailyOutputs();

    /**
     * the merging request file
     */
    @Embedded
    private DailyInputs dailyInputs = new DailyInputs();

}

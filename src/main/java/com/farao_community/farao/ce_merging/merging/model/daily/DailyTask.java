/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.model.daily;

import com.farao_community.farao.ce_merging.common.model.TaskStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.model.TaskStatus.CREATED;
import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.GenerationType.AUTO;

/**
 * WARNING: this class is linked to the merging supervisor. Please contact them if any modification is needed to check compatibility
 */
@Entity
public class DailyTask implements Serializable {
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
     * The Status of the merging Task
     */
    private TaskStatus taskStatus = CREATED;

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

    public long getDailyMergingTaskId() {
        return dailyMergingTaskId;
    }

    public void setDailyMergingTaskId(final long dailyMergingTaskId) {
        this.dailyMergingTaskId = dailyMergingTaskId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(final TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }

    public List<Long> getCeMergingTaskIdsList() {
        return ceMergingTaskIdsList;
    }

    public void setCeMergingTaskIdsList(final List<Long> ceMergingTaskIdsList) {
        this.ceMergingTaskIdsList = ceMergingTaskIdsList;
    }

    public DailyOutputs getDailyOutputs() {
        return dailyOutputs;
    }

    public void setDailyOutputs(final DailyOutputs dailyOutputs) {
        this.dailyOutputs = dailyOutputs;
    }

    public DailyInputs getDailyInputs() {
        return dailyInputs;
    }

    public void setDailyInputs(final DailyInputs dailyInputs) {
        this.dailyInputs = dailyInputs;
    }
}

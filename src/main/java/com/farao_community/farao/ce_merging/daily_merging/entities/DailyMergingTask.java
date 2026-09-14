/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.entities;

import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class DailyMergingTask implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private int version;

    private TaskStatus taskStatus = TaskStatus.CREATED;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Long> mergingTaskIds = new ArrayList<>();

    @Embedded
    private DailyOutputs dailyOutputs = new DailyOutputs();

    @Embedded
    private DailyInputs dailyInputs = new DailyInputs();

    public Long getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<Long> getMergingTaskIds() {
        return mergingTaskIds;
    }

    public void setMergingTaskIds(List<Long> ids) {
        this.mergingTaskIds = ids;
    }

    public DailyOutputs getDailyOutputs() {
        return dailyOutputs;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public void setDailyOutputs(DailyOutputs dailyOutputs) {
        this.dailyOutputs = dailyOutputs;
    }

    public DailyInputs getDailyInputs() {
        return dailyInputs;
    }

    public void setDailyInputs(DailyInputs dailyInputs) {
        this.dailyInputs = dailyInputs;
    }

    public void setTaskStatus(TaskStatus taskStatus) {
        this.taskStatus = taskStatus;
    }
}

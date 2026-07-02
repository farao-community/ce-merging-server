/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.task;

import com.farao_community.farao.ce_merging.common.json_api.JsonApiData;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeDeserializer;
import com.farao_community.farao.ce_merging.common.serialize.OffsetDateTimeSerializer;
import com.farao_community.farao.ce_merging.common.task.Task;
import com.farao_community.farao.ce_merging.common.task.TaskStatus;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_WINTER_OFFSET;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.CREATED;
import static jakarta.persistence.GenerationType.AUTO;

@Entity
public class BciTask implements JsonApiData, Task {

    @Id
    @GeneratedValue(strategy = AUTO)
    private Long id;

    private String type = "tasks";

    private String name;

    @JsonSerialize(using = OffsetDateTimeSerializer.class)
    @JsonDeserialize(using = OffsetDateTimeDeserializer.class)
    @Column(name = "local_date_time", columnDefinition = "TIMESTAMP")
    private OffsetDateTime processTargetDate;

    private ZoneOffset realOffset;

    @Embedded
    private BciInputs bciInputs;

    @Embedded
    private BciOutput bciOutput;

    private TaskStatus status;

    @Lob
    private String regionConfiguration;

    public BciTask() {
        status = CREATED;
        bciInputs = null;
        bciOutput = null;
        name = null;
        processTargetDate = null;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public BciInputs getBciInputs() {
        return bciInputs;
    }

    public void setBciInputs(final BciInputs bciInputs) {
        this.bciInputs = bciInputs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BciOutput getBciOutput() {
        return bciOutput;
    }

    public ZoneOffset getRealOffset() {
        return Optional.ofNullable(realOffset)
            .orElse(PARIS_WINTER_OFFSET);
    }

    public void setRealOffset(final ZoneOffset realOffset) {
        this.realOffset = realOffset;
    }

    public void setBciOutput(final BciOutput bciOutput) {
        this.bciOutput = bciOutput;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(final TaskStatus bciStatus) {
        this.status = bciStatus;
    }

    public OffsetDateTime getProcessTargetDate() {
        return processTargetDate;
    }

    public void setProcessTargetDate(final OffsetDateTime processTargetDate) {
        this.processTargetDate = processTargetDate;
    }

    public String getRegionConfiguration() {
        return regionConfiguration;
    }

    public void setRegionConfiguration(final String regionConfiguration) {
        this.regionConfiguration = regionConfiguration;
    }
}

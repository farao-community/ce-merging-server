/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process;

import com.farao_community.farao.ce_merging.common.GenericMergingRequest;
import com.farao_community.farao.ce_merging.merging.model.daily.DailyTask;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.MergingTask;

import java.time.OffsetDateTime;
import java.util.List;

public class PostProcessRequest implements GenericMergingRequest {
    private DailyTask dailyCeMergingEntity;
    private List<MergingTask> mergingTasks;
    private String requestTimeInterval;
    private OffsetDateTime mergingDay;
    private String noun;
    private String context;
    private String replyAddress;
    private String correlationID;

    public PostProcessRequest(final String context,
                              final DailyTask dailyCeMergingEntity,
                              final List<MergingTask> mergingTasks,
                              final String requestTimeInterval,
                              final OffsetDateTime mergingDay,
                              final String noun,
                              final String replyAddress,
                              final String correlationID) {
        this.context = context;
        this.dailyCeMergingEntity = dailyCeMergingEntity;
        this.mergingTasks = mergingTasks;
        this.requestTimeInterval = requestTimeInterval;
        this.mergingDay = mergingDay;
        this.noun = noun;
        this.replyAddress = replyAddress;
        this.correlationID = correlationID;
    }

    @Override
    public String getNoun() {
        return noun;
    }

    @Override
    public void setNoun(final String noun) {
        this.noun = noun;
    }

    public DailyTask getDailyCeMergingEntity() {
        return dailyCeMergingEntity;
    }

    public void setDailyCeMergingEntity(final DailyTask dailyCeMergingEntity) {
        this.dailyCeMergingEntity = dailyCeMergingEntity;
    }

    public List<MergingTask> getMergingTasks() {
        return mergingTasks;
    }

    public void setMergingTasks(final List<MergingTask> mergingTasks) {
        this.mergingTasks = mergingTasks;
    }

    @Override
    public String getRequestTimeInterval() {
        return requestTimeInterval;
    }

    @Override
    public void setRequestTimeInterval(final String requestTimeInterval) {
        this.requestTimeInterval = requestTimeInterval;
    }

    @Override
    public OffsetDateTime getMergingDay() {
        return mergingDay;
    }

    @Override
    public void setMergingDay(final OffsetDateTime mergingDay) {
        this.mergingDay = mergingDay;
    }

    @Override
    public String getContext() {
        return context;
    }

    @Override
    public void setContext(final String context) {
        this.context = context;
    }

    @Override
    public String getReplyAddress() {
        return replyAddress;
    }

    @Override
    public void setReplyAddress(final String replyAddress) {
        this.replyAddress = replyAddress;
    }

    @Override
    public String getCorrelationID() {
        return correlationID;
    }

    @Override
    public void setCorrelationID(final String correlationID) {
        this.correlationID = correlationID;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.post_process;

import com.farao_community.farao.ce_merging.common.GenericMergingRequest;
import com.farao_community.farao.ce_merging.daily_aggregation.entities.DailyTask;
import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PostProcessRequest implements GenericMergingRequest {
    private final DailyTask dailyCeMergingEntity;
    private final List<MergingTask> mergingTasks;
    private String requestTimeInterval;
    private OffsetDateTime mergingDay;
    private String noun;
    private String context;
    private String replyAddress;
    private String correlationID;
}

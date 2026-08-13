/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.handlers;

import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.post_process.PostProcessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.convertToZFormat;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Comparator.comparing;

@Component
@Order(20)
public class O2CheckDatesValidity implements Handler<PostProcessRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(O2CheckDatesValidity.class);

    @Override
    public boolean handle(final PostProcessRequest request) {
        checkIfTargetDatesInRequestInterval(request);
        checkIfDistinctTargetDates(request.getMergingTasks());
        final Comparator<MergingTask> byTargetDateTime = comparing(MergingTask::getTargetDate);
        request.getMergingTasks().sort(byTargetDateTime);
        return false;
    }

    private void checkIfTargetDatesInRequestInterval(final PostProcessRequest request) {
        request.getMergingTasks()
                .stream()
                .filter(task -> !task.isBetween(request.getStartDateTime(), request.getEndDateTime()))
                .findAny()
                .ifPresent(task -> {
                    String errorMessage = String.format("Task's %s target date %s outside merging request time interval %s",
                                                        task.getId(),
                                                        task.getTargetDate(),
                                                        request.getRequestTimeInterval());
                    LOGGER.error(errorMessage);
                    throw new CeMergingException(errorMessage);
                });

    }

    private void checkIfDistinctTargetDates(final List<MergingTask> tasksList) {
        final Set<String> tasksIntervals = new HashSet<>();

        tasksList.stream()
                .map(task -> task.getTargetDate().truncatedTo(HOURS))
                .map(dt -> convertToZFormat(dt) + "/" + convertToZFormat(dt.plusHours(1)))
                .forEach(interval -> {
                    if (!tasksIntervals.add(interval)) {
                        final String errorMessage = String.format("More than one task with same target date inside interval %s",
                                                                  interval);
                        LOGGER.error(errorMessage);
                        throw new CeMergingException(errorMessage);
                    }
                });
    }

}

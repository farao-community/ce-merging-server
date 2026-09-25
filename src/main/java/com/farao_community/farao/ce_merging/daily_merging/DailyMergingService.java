/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.MergingRequestService;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;

import com.farao_community.farao.ce_merging.daily_merging.outputs.cgm.CgmResultsService;
import com.farao_community.farao.ce_merging.daily_merging.outputs.glsk_quality_check.DailyQualityCheckReportService;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_logs.DailyMergingLogsService;
import com.farao_community.farao.ce_merging.daily_merging.outputs.merging_response.MergingResponseService;
import com.farao_community.farao.ce_merging.daily_merging.outputs.refprog.DailyRefProgService;
import com.farao_community.farao.ce_merging.daily_merging.outputs.xnodes_inconsistencies.XnodeResultService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;

@Service
public class DailyMergingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyMergingService.class);

    private final DailyQualityCheckReportService dailyQualityCheckReportService;
    private final MergingRequestService mergingRequestService;
    private final MergingResponseService mergingResponseService;
    private final DailyMergingLogsService dailyMergingLogsService;
    private final XnodeResultService xnodeResultService;
    private final CgmResultsService cgmResultsService;
    private final DailyRefProgService dailyRefProgService;

    public DailyMergingService(final DailyQualityCheckReportService dailyQualityCheckReportService,
                               final DailyMergingLogsService dailyMergingLogsService,
                               final MergingRequestService mergingRequestService,
                               final MergingResponseService mergingResponseService,
                               final XnodeResultService xnodeResultService,
                               final CgmResultsService cgmResultsService,
                               final DailyRefProgService dailyRefProgService) {
        this.dailyMergingLogsService = dailyMergingLogsService;
        this.dailyQualityCheckReportService = dailyQualityCheckReportService;
        this.mergingRequestService = mergingRequestService;
        this.mergingResponseService = mergingResponseService;
        this.xnodeResultService = xnodeResultService;
        this.cgmResultsService = cgmResultsService;
        this.dailyRefProgService = dailyRefProgService;
    }

    public void run(final DailyMergingTask dailyMergingTask,
                    final List<MergingTask> hourlyTasks) {
        final RequestInformation requestInformation = mergingRequestService.getMergingRequestInformation(dailyMergingTask);
        validateTaskTargetDatesWithinRequestInterval(hourlyTasks, requestInformation);
        checkTasksHaveDifferentTargetDates(hourlyTasks);
        hourlyTasks.sort(Comparator.comparing(task -> task.getInputs().getTargetDate()));
        final List<MergingTask> successMergingTasks = hourlyTasks.stream()
                .filter(task -> task.getStatus() == SUCCESS)
                .toList();

        if (!successMergingTasks.isEmpty()) {
            dailyRefProgService.computeDailyRefProg(dailyMergingTask, successMergingTasks, requestInformation);
            dailyMergingLogsService.computeDailyMergingLogs(dailyMergingTask, successMergingTasks);
            dailyQualityCheckReportService.computeDailyGlskQualityReport(dailyMergingTask, successMergingTasks, requestInformation.requestTimeInterval());
        }
        mergingResponseService.computeMergingResponse(dailyMergingTask, successMergingTasks, requestInformation);
        cgmResultsService.createCgmZip(dailyMergingTask, hourlyTasks, requestInformation);
        xnodeResultService.createXnodesInconsistenciesZip(dailyMergingTask, successMergingTasks); //workaround as xnodes file are not yet available on Merging supervisor

    }

    private void validateTaskTargetDatesWithinRequestInterval(final List<MergingTask> tasks,
                                                              final RequestInformation requestInformation) {
        tasks.forEach(task -> validateTaskTargetDateWithinRequestInterval(task, requestInformation));
    }

    private void validateTaskTargetDateWithinRequestInterval(final MergingTask task,
                                                             final RequestInformation requestInformation) {
        final OffsetDateTime targetDate = task.getInputs().getTargetDate();
        final OffsetDateTime requestStartDateTime = requestInformation.getStartDateTime();
        final OffsetDateTime requestEndDateTime = requestInformation.getEndDateTime();

        if (!(targetDate.isAfter(requestStartDateTime) && targetDate.isBefore(requestEndDateTime.minusMinutes(1)))) {
            final String errorMessage = String.format(
                    "Task's %s target date %s outside merging request time interval %s",
                    task.getId(),
                    targetDate,
                    requestInformation.requestTimeInterval()
            );
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage);
        }
    }

    private void checkTasksHaveDifferentTargetDates(final List<MergingTask> tasks) {
        final Set<OffsetDateTime> targetDateIntervals = new HashSet<>();
        for (final MergingTask task : tasks) {
            final OffsetDateTime targetDate = task.getInputs().getTargetDate();
            final OffsetDateTime targetDateInterval = targetDate.truncatedTo(ChronoUnit.HOURS);
            if (!targetDateIntervals.add(targetDateInterval)) {
                final String timeInterval = DateTimeUtils.toHourlyInterval(targetDateInterval);
                final String errorMessage = String.format("More than one task with same target date inside interval %s", timeInterval);
                LOGGER.error(errorMessage);
                throw new CeMergingException(errorMessage);
            }
        }
    }
}

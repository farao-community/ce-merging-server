/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.outputs.merging_logs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.post_process.common.SchemaLocationNamespace;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_DATE_TIME_END_INDEX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_DAY_START_INDEX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XML_EXTENSION;

@Service
public class DailyMergingLogsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DailyMergingLogsService.class);
    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;
    private static final int FLOW = 123;

    public DailyMergingLogsService(CeMergingConfiguration configuration, DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void computeDailyMergingLogs(DailyMergingTask dailyMergingTask, List<MergingTask> mergingTasks) {
        try {
            final MergingLog mergingLog = buildDailyMergingLogs(dailyMergingTask.getVersion(), mergingTasks);
            saveDailyMergingLogsInOutputs(dailyMergingTask, mergingLog);
            repository.save(dailyMergingTask);
        } catch (Exception e) {
            LOGGER.error("Error during creation of daily merging logs file for task '{}' ", dailyMergingTask.getId(), e);
            throw new CeMergingException("Error during creation of daily merging logs file", e);
        }
    }

    public MergingLog buildDailyMergingLogs(int version, final List<MergingTask> mergingTasks) {
        final List<MergingLog> mergingLogsList = getAllMergingLogs(mergingTasks);
        final MergingLog dailyMergingLogs = mergingLogsList.getFirst();
        updateHeader(dailyMergingLogs, version);
        mergingLogsList.stream()
                .skip(1)
                .map(MergingLog::getTimeSeries)
                .map(MergingLog.TimeSeries::getPeriod)
                .flatMap(period -> period.getInterval().stream())
                .forEach(interval -> addIntervalToMergingLog(interval, dailyMergingLogs));
        return dailyMergingLogs;
    }

    List<MergingLog> getAllMergingLogs(final List<MergingTask> mergingTasks) {
        return mergingTasks.stream()
                .map(task -> task.getOutputs().getMergingLogs())
                .map(file -> JaxbUtils.readFromPath(MergingLog.class, file.getPath()))
                .toList();
    }

    boolean positionNotPresentInMergingLog(final Integer position, final List<MergingLog.TimeSeries.Period.Interval> intervals) {
        return intervals.stream()
                .map(interval -> Integer.valueOf(interval.getPos().getV()))
                .noneMatch(position::equals);
    }

    private void saveDailyMergingLogsInOutputs(final DailyMergingTask dailyMergingTask, final MergingLog mergingLog) {
        try {
            final OffsetDateTime mergingDate = OffsetDateTime.parse(mergingLog.getReportTimeInterval().getV().substring(MERGING_DAY_START_INDEX, MERGING_DATE_TIME_END_INDEX), DateTimeFormatter.ISO_DATE_TIME);
            final String mergingLogsFileName = OutputUtils.generateOutputFileName(mergingDate, dailyMergingTask.getVersion(), FLOW, XML_EXTENSION);
            final String fileLocation = String.format("/daily-merging/tasks/%d/outputs/merging-logs", dailyMergingTask.getId());
            final SavedFile dailyMergingLogsSavedFile = FileStorageUtils.save(
                    configuration.getDailyOutputsDirectoryPath(dailyMergingTask),
                    mergingLogsFileName,
                    fileLocation,
                    path -> JaxbUtils.writeToPath(
                            MergingLog.class,
                            mergingLog,
                            path,
                            SchemaLocationNamespace.MERGING_LOGS_XSD.getName(),
                            false
                    )
            );
            dailyMergingTask.getDailyOutputs().setMergingLogs(dailyMergingLogsSavedFile);

        } catch (final Exception e) {
            LOGGER.error("Error during saving Merging logs for task '{}'", dailyMergingTask.getId(), e);
            throw new CeMergingException("Error during saving Merging logs", e);
        }
    }

    private void updateHeader(final MergingLog mergingLog, final int version) {
        final MergingLog.CreationDateTime creationDateTime = new MergingLog.CreationDateTime();
        creationDateTime.setV(DateTimeUtils.getNowDate());
        mergingLog.setCreationDateTime(creationDateTime);
        final MergingLog.DocumentVersion documentVersion = new MergingLog.DocumentVersion();
        documentVersion.setV((byte) version);
        mergingLog.setDocumentVersion(documentVersion);
        final String documentId = String.format(
                "%s-F%d-%02d",
                mergingLog.getDocumentIdentification().getV(),
                FLOW,
                version
        );
        final MergingLog.DocumentIdentification documentIdentification = new MergingLog.DocumentIdentification();
        documentIdentification.setV(documentId);
        mergingLog.setDocumentIdentification(documentIdentification);
    }

    private void addIntervalToMergingLog(final MergingLog.TimeSeries.Period.Interval interval, final MergingLog mergingLog) {
        final List<MergingLog.TimeSeries.Period.Interval> intervals = mergingLog.getTimeSeries().getPeriod().getInterval();
        final Integer position = Integer.valueOf(interval.getPos().getV());
        if (positionNotPresentInMergingLog(position, intervals)) {
            intervals.add(interval);
        }
    }

}

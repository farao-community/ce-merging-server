/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.output.merging_report;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.OutputUtils.OUTPUT_DATE_FORMATTER;

@Service
public class MergingReportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingReportService.class);
    private static final String MERGING_REPORT_FILE_NAME = "MERGING_REPORT_%s.xlsx";
    private final CeMergingConfiguration configuration;
    private final DailyMergingRepository repository;

    public MergingReportService(final CeMergingConfiguration configuration,
                                final DailyMergingRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    public void computeMergingReport(final DailyMergingTask dailyTask,
                                     final List<MergingTask> hourlyTasks,
                                     final RequestInformation requestInformation) {
        final String reportFileName = String.format(MERGING_REPORT_FILE_NAME, requestInformation.getMergingDay().format(OUTPUT_DATE_FORMATTER));
        final String filePath = Paths.get(configuration.getDailyOutputsDirectoryPath(dailyTask), reportFileName).toString();

        new MergingReportBuilder(hourlyTasks, requestInformation).handle(filePath);
        updateDailyTask(dailyTask, filePath, reportFileName);
    }

    private void updateDailyTask(final DailyMergingTask dailyTask,
                                 final String filePath,
                                 final String reportFileName) {
        final SavedFile mergingReportSavedFile = new SavedFile(
                reportFileName, filePath, String.format("/daily-merging/tasks/%d/outputs/merging-report", dailyTask.getId())
        );
        dailyTask.getDailyOutputs().setMergingReport(mergingReportSavedFile);
        repository.save(dailyTask);
        LOGGER.info("File '{}' is saved in task '{}' outputs", reportFileName, dailyTask.getId());
    }
}

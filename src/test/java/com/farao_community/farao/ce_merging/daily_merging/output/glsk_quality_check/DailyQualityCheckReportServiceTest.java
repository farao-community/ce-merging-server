/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.glsk_quality_check;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QualityCheckReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DailyQualityCheckReportServiceTest {
    private final Path resourceDirectory = Paths.get("src", "test", "resources", "glskFix");

    @Autowired
    private DailyMergingRepository dailyCoreMergingRepository;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    private DailyQualityCheckReportService dailyQualityCheckReportService;

    private MergingTask task1;
    private MergingTask task2;
    private MergingTask task3;

    @BeforeEach
    void setUp() {
        task1 = createTask("2016-07-28T22:30:00Z", "20160728_2230_GLSK_QUALITY_REPORT_1.xml");
        task2 = createTask("2016-07-28T23:30:00Z", "20160728_2330_GLSK_QUALITY_REPORT_2.xml");
        task3 = createTask("2016-07-29T00:30:00Z", "20160729_0030_GLSK_QUALITY_REPORT_3.xml");
    }

    @Test
    void shouldBuildOneDayGlskReportFromMultipleTasks() {
        final List<MergingTask> tasks = Arrays.asList(task1, task2, task3);
        final String requestInterval = "2016-07-28T22:00Z/2016-07-29T01:00Z";
        final QualityCheckReport report = dailyQualityCheckReportService.buildDailyGlskReport(1, tasks, requestInterval);
        assertEquals(3, report.getQualityChecks().size());
        assertEquals(2, report.getQualityChecks().get(0).getQualityCheck().size());
        assertEquals("2016-07-28T22:00Z/2016-07-28T23:00Z", report.getQualityChecks().get(0).getQualityCheck().get(0).getTimeInterval());
        assertEquals(1, report.getQualityChecks().get(1).getQualityCheck().size());
        assertEquals(1, report.getQualityChecks().get(2).getQualityCheck().size());
        assertEquals(requestInterval, report.getQualityCheckTimeInterval().getV());
    }

    @Test
    void shouldComputeAndSaveGlskQualityReport() throws IOException {
        final DailyMergingTask dailyCoreMergingEntity = createDailyMergingTask();
        final List<MergingTask> tasks = Arrays.asList(task1, task2);
        final String requestInterval = "2016-07-28T22:00Z/2016-07-29T22:00Z";

        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyCoreMergingEntity)));
        dailyQualityCheckReportService.computeDailyGlskQualityReport(dailyCoreMergingEntity, tasks, requestInterval);

        final SavedFile report = dailyCoreMergingEntity.getDailyOutputs().getGlskQualityReport();
        assertTrue(new File(report.getPath()).exists());
        assertEquals("22XCORESO------S_10V1001C--00236Y_CORE-FB-A16A48-117_20160729-F117-04.xml", report.getOriginalName());
    }

    private MergingTask createTask(final String targetDate, final String fileName) {
        final SavedFile report = new SavedFile(fileName, resourceDirectory.resolve(fileName).toString(), "mock");
        final MergingTask task = new MergingTask();
        task.getInputs().setTargetDate(OffsetDateTime.parse(targetDate));
        task.getArtifacts().putFile(ArtifactType.GLSK_QUALITY_REPORT, report);
        return task;
    }

    private DailyMergingTask createDailyMergingTask() {
        final DailyMergingTask dailyMergingTask = new DailyMergingTask();
        dailyMergingTask.setVersion(4);
        dailyMergingTask.setDailyInputs(new DailyInputs());
        return dailyCoreMergingRepository.save(dailyMergingTask);
    }
}



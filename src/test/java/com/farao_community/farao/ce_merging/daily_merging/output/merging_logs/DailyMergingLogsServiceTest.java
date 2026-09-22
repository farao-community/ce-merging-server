/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.merging_logs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DailyMergingLogsServiceTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "mergingLogs");

    private MergingTask task1;
    private MergingTask task2;
    private MergingTask task3;

    @Autowired
    private DailyMergingRepository repository;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    private DailyMergingLogsService service;

    @BeforeEach
    void setUp() {
        task1 = createTask(1L, "mergingLogs1.xml");
        task2 = createTask(2L, "mergingLogs2.xml");
        task3 = createTask(3L, "mergingLogs3.xml");
    }

    @Test
    void shouldCreateExpectedIntervals() {
        final int version = 1;
        final List<MergingTask> tasks = List.of(task1, task2, task3);
        final MergingLog mergingLog = service.buildDailyMergingLogs(version, tasks);
        assertNotNull(mergingLog);
        assertNotNull(mergingLog.getDocumentVersion());
        assertNotNull(mergingLog.getTimeSeries());
        assertNotNull(mergingLog.getTimeSeries().getPeriod());
        assertEquals(version, mergingLog.getDocumentVersion().getV().intValue());
        final List<MergingLog.TimeSeries.Period.Interval> intervals = mergingLog.getTimeSeries().getPeriod().getInterval();
        assertEquals(3, intervals.size());
        assertEquals(1, Integer.parseInt(intervals.get(0).getPos().getV()));
        assertEquals(2, Integer.parseInt(intervals.get(1).getPos().getV()));
        assertEquals(3, Integer.parseInt(intervals.get(2).getPos().getV()));
    }

    @Test
    void shouldReturnAllMergingLogs() {
        final List<MergingTask> tasks = List.of(task1, task2, task3);
        final List<MergingLog> mergingLogs = service.getAllMergingLogs(tasks);
        assertNotNull(mergingLogs);
        assertEquals(3, mergingLogs.size());
        mergingLogs.forEach(mergingLog -> assertNotNull(mergingLog));
    }

    @Test
    void shouldReturnFalseWhenPositionExists() {
        final List<MergingLog.TimeSeries.Period.Interval> intervals = List.of(intervalWithPosition(1), intervalWithPosition(2));
        assertFalse(service.positionNotPresentInMergingLog(1, intervals));
        assertFalse(service.positionNotPresentInMergingLog(2, intervals));
    }

    @Test
    void shouldReturnTrueWhenPositionDoesNotExist() {
        final List<MergingLog.TimeSeries.Period.Interval> intervals = List.of(intervalWithPosition(1), intervalWithPosition(2));
        assertTrue(service.positionNotPresentInMergingLog(3, intervals));
    }

    @Test
    void shouldSaveDailyMergingLogs() throws IOException {
        final int version = 4;
        DailyMergingTask dailyMergingTask = new DailyMergingTask();
        final DailyInputs dailyInputs = new DailyInputs();
        dailyMergingTask.setVersion(version);
        dailyMergingTask.setDailyInputs(dailyInputs);
        dailyMergingTask = repository.save(dailyMergingTask);
        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyMergingTask)));
        final List<MergingTask> tasks = List.of(task1, task2, task3);
        service.computeDailyMergingLogs(dailyMergingTask, tasks);
        assertNotNull(dailyMergingTask.getDailyOutputs());
        assertNotNull(dailyMergingTask.getDailyOutputs().getMergingLogs());
        final SavedFile mergingLogs = dailyMergingTask.getDailyOutputs().getMergingLogs();
        assertNotNull(mergingLogs);
        assertEquals("22XCORESO------S_10V1001C--00236Y_CORE-FB-123_20200219-F123-04.xml", mergingLogs.getOriginalName());
        assertTrue(Files.exists(Path.of(mergingLogs.getPath())));

    }

    private MergingTask createTask(final long id, final String mergingLogFileName) {
        final Path mergingLogPath = RESOURCE_DIRECTORY.resolve(mergingLogFileName);
        final SavedFile mergingLog = new SavedFile(mergingLogFileName, mergingLogPath.toAbsolutePath().toString(), "mock");
        final MergingTask task = new MergingTask();
        task.setId(id);
        final Outputs outputs = new Outputs();
        outputs.setMergingLogs(mergingLog);
        task.setOutputs(outputs);
        return task;
    }

    private MergingLog.TimeSeries.Period.Interval intervalWithPosition(final int position) {
        final MergingLog.TimeSeries.Period.Interval interval = new MergingLog.TimeSeries.Period.Interval();
        final MergingLog.TimeSeries.Period.Interval.Pos pos = new MergingLog.TimeSeries.Period.Interval.Pos();
        pos.setV(String.valueOf(position));
        interval.setPos(pos);
        return interval;
    }
}

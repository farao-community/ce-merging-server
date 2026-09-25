/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.merging_report;

import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyInputs;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.daily_merging.output.merging_report.MergingReportBuilder;
import com.farao_community.farao.ce_merging.daily_merging.output.merging_report.MergingReportService;
import com.farao_community.farao.ce_merging.daily_merging.output.merging_report.sheets.FilesSheet;
import com.farao_community.farao.ce_merging.daily_merging.output.merging_report.sheets.MergeSheet;
import com.farao_community.farao.ce_merging.daily_merging.output.merging_report.sheets.XNodeInconsistenciesSheet;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.AC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.IgmType.D2CF;
import static com.farao_community.farao.ce_merging.merging.task.enums.IgmType.DACF;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test_utils.CeTestUtils.createMockTask;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.withTargetDateAndIgms;

@SpringBootTest
class MergingReportServiceTest {
    private DailyMergingTask dailyTask;
    private List<MergingTask> tasksList;
    private RequestInformation requestInformation;
    private MergingReportBuilder builder;

    @Autowired
    private DailyMergingRepository repository;

    @Autowired
    MergingReportService mergingReportService;

    @BeforeEach
    void setUp() {
        final SavedFile xnodesInconsistencies = new SavedFile("xnodesInconsistencies.json", stringPathOf("merging_report/xnodesInconsistencies.json"), "mock");
        final SavedFile cgmNetPositionsFile = new SavedFile("cgmNetPositions.json", stringPathOf("merging_report/cgmNetPositions.json"), "mock");
        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(XNODES_INCONSISTENCIES, xnodesInconsistencies);
        artifacts.putFile(CGM_NET_POSITIONS_FILE, cgmNetPositionsFile);

        final MergingTask task1 = createMockTask(1L,
                                                 withTargetDateAndIgms("2020-01-06T01:30Z",
                                                                       Map.of("CH", "mock/20200922_0430_FO2_CH0.UCT",
                                                                              "IT", "mock/20200923_0430_2D1_IT4.uct")),
                                                 artifacts,
                                                 SUCCESS);

        final MergingTask task2 = createMockTask(2L,
                                                 withTargetDateAndIgms("2020-01-06T02:30Z", Map.of()),
                                                 artifacts,
                                                 ERROR);
        task2.setStatusDetail("ERROR CAUSE");

        dailyTask = new DailyMergingTask();
        final DailyInputs dailyInputs = new DailyInputs();
        dailyTask.setDailyInputs(dailyInputs);
        tasksList = List.of(task1, task2);
        requestInformation = new RequestInformation("2020-01-05T23:00Z/2020-01-06T23:00Z",
                                                    "MergingCommonGridModel",
                                                    "PRODUCTION",
                                                    "22XCORESO------S",
                                                    "62613970-472b-405a-b8c9-9e1be581f519");
        builder = new MergingReportBuilder(tasksList, requestInformation);
    }

    @Test
    public void shouldProduceMergingReport() {
        dailyTask = repository.save(dailyTask);
        mergingReportService.computeMergingReport(dailyTask, tasksList, requestInformation);
        assertNotNull(dailyTask.getDailyOutputs().getMergingReport());
    }

    @Test
    public void shouldProduceXNodeInconsistenciesSheets() {
        final List<XNodeInconsistenciesSheet> xNodeInconsistenciesSheets = builder.getXNodeInconsistenciesSheets();
        assertEquals(4, xNodeInconsistenciesSheets.size());
        final XNodeInconsistenciesSheet first = xNodeInconsistenciesSheets.getFirst();

        assertEquals("20200106", first.bd());
        assertEquals("3", first.timestamp());
        assertEquals("XAB_1234", first.xNode());
        assertEquals("BE", first.tso1());
        assertEquals("CLOSE", first.status1());
        assertEquals("FR", first.tso2());
        assertEquals("OPEN", first.status2());
        assertEquals("OPEN", first.finalStatus());
        assertEquals("auto", first.correctionsApplied());
    }

    @Test
    public void shouldProduceFilesSheetsWithIgmInfo() {
        final List<FilesSheet> filesSheets = builder.getFilesSheets();
        assertEquals(2, filesSheets.size());
        final FilesSheet first = filesSheets.getFirst();
        final FilesSheet last = filesSheets.getLast();

        assertEquals("20200106", first.bd());
        assertEquals("3", first.timestamp());
        assertEquals(DACF.name(), first.igmCH());
        assertEquals(D2CF.name(), first.igmIT());

        assertEquals("20200106", last.bd());
        assertEquals("4", last.timestamp());
        assertEquals(DACF.name(), last.igmCH());
        assertEquals(DACF.name(), last.igmIT());
    }

    @Test
    public void shouldProduceMergeSheetsWithLoadFlowStatuses() {
        final List<MergeSheet> mergeSheets = builder.getMergeSheets();
        assertEquals(2, mergeSheets.size());
        final MergeSheet first = mergeSheets.getFirst();
        final MergeSheet last = mergeSheets.getLast();

        assertEquals("20200106", first.bd());
        assertEquals("3", first.timestamp());
        assertEquals(AC, first.type());
        assertEquals(9, first.lfIterations());
        assertEquals(4.8, first.slackImbalance(), .01);
        assertEquals(EMPTY, first.justification());
        assertEquals(EMPTY, first.correctionsApplied());

        assertEquals("20200106", last.bd());
        assertEquals("4", last.timestamp());
        assertEquals("Failure", last.type());
        assertEquals(9, last.lfIterations());
        assertEquals(4.8, last.slackImbalance(), .01);
        assertEquals("ERROR CAUSE", last.justification());
        assertEquals(EMPTY, last.correctionsApplied());
    }

}

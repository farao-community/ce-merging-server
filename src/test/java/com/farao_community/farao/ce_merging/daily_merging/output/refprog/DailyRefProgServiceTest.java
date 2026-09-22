/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.refprog;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.mockTaskWithRefProgResult;
import static test_utils.assertions.TimeSeriesAssert.assertThat;

@SpringBootTest
class DailyRefProgServiceTest {
    private MergingTask task1;
    private MergingTask task2;
    private MergingTask task3;
    private MergingTask task4;
    private MergingTask task5;

    @Autowired
    private DailyMergingRepository repository;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    private DailyRefProgService builder;

    @BeforeEach
    void setUp() {
        task1 = mockTaskWithRefProgResult("2020-01-06T20:30Z", "/refProg/refProg1.xml");
        task2 = mockTaskWithRefProgResult("2020-01-06T21:30Z", "/refProg/refProg2.xml");
        task3 = mockTaskWithRefProgResult("2020-01-06T22:30Z", "/refProg/20200106_2230_CORESO_RefProg.xml");
        task4 = mockTaskWithRefProgResult("2020-01-06T23:30Z", "/refProg/20200106_2330_CORESO_RefProg.xml");
        task5 = mockTaskWithRefProgResult("2019-06-18T00:30Z", "/refProg/20190618_0030_CORESO_RefProg.xml");
    }

    @Test
    void shouldBuildDailyRefProg() {
        final List<MergingTask> tasksList = Arrays.asList(task1, task2);
        final PublicationDocument publicationDocument = builder.buildDailyRefProg(1, tasksList, 24);
        assertEquals(1, publicationDocument.getDtdVersion().intValue());
        assertEquals(0, publicationDocument.getDtdRelease().intValue());

        final List<PublicationDocument.PublicationTimeSeries> docTimeSeries = publicationDocument.getPublicationTimeSeries();

        assertEquals(68, docTimeSeries.size());

        assertThat(docTimeSeries.getFirst())
                .isIdentifiedBy("RS-ME")
                .links("10YCS-SERBIATSOV", "10YCS-CG-TSO---S")
                .isFullDay()
                .hasPoint(22, 1238)
                .hasPoint(24, -900);

        assertThat(docTimeSeries.get(1))
                .isIdentifiedBy("GR-MK")
                .links("10YCB-GREECE---2", "10YMK-MEPSO----8")
                .isFullDay()
                .hasPoint(22, -3088)
                .hasPoint(24, -3355);

        assertThat(docTimeSeries.get(65))
                .isIdentifiedBy("NL-DK1_Cobra")
                .links("10YNL----------L", "17YXXXXXXAAAAAAB")
                .isFullDay()
                .hasPoint(22, -2978)
                .hasPoint(24, 2594);
    }

    @Test
    void shouldExportBorderWhenBorderPresentForTaskButNotForAnother() {
        final List<MergingTask> tasksList = Arrays.asList(task3, task4);
        final PublicationDocument publicationDocument = builder.buildDailyRefProg(1, tasksList, 24);
        final List<String> timeSeriesIdentificationList = publicationDocument.getPublicationTimeSeries()
                .stream()
                .map(publication -> publication.getTimeSeriesIdentification().getV())
                .toList();
        assertTrue(timeSeriesIdentificationList.contains("RS-ME"));
    }

    @Test
    void shouldThrowExceptionWhenTasksHaveDifferentInterval() {
        final List<MergingTask> tasksList = Arrays.asList(task4, task5);
        final List<PublicationDocument> refProgResultsList = builder.getAllHourlyRefProgs(tasksList);
        assertThrows(CeMergingException.class, () -> {
            builder.checkUniqueRefProgInterval(refProgResultsList);
        });
    }

    @Test
    void shouldNotThrowExceptionWhenTasksHaveSameInterval() {
        final List<MergingTask> tasksList = Arrays.asList(task1, task2, task3, task4);
        final List<PublicationDocument> refProgResultsList = builder.getAllHourlyRefProgs(tasksList);
        builder.checkUniqueRefProgInterval(refProgResultsList);
    }

    @Test
    void shouldSaveDailyRefProg() throws IOException {
        DailyMergingTask dailyTask = new DailyMergingTask();
        final int version = 4;
        dailyTask.setVersion(version);
        dailyTask = repository.save(dailyTask);
        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyTask)));
        final List<MergingTask> tasksList = Arrays.asList(task1, task2);
        final RequestInformation requestInformation = new RequestInformation("2020-01-05T23:00Z/2020-01-06T23:00Z",
                                                                       "regprog",
                                                                       "test",
                                                                       "CORESO",
                                                                       "123456"
        );
        builder.computeDailyRefProg(dailyTask, tasksList, requestInformation);
        assertTrue(new File(dailyTask.getDailyOutputs().getRefProg().getPath()).exists());
        final String expectedName = "22XCORESO------S_10V1001C--00236Y_CORE-FB-A45-101_20200106-F101-04.xml";
        assertEquals(expectedName, dailyTask.getDailyOutputs().getRefProg().getOriginalName());
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.daily_merging.output.cgm;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.merging_request.RequestInformation;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;

import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class CgmResultsServiceTest {

    @Autowired
    private CeMergingConfiguration configuration;

    private final DailyMergingRepository tasksRepository = mock(DailyMergingRepository.class);

    private DailyMergingTask dailyMergingTask;
    private final CgmRecognitionService cgmRecognitionService = mock(CgmRecognitionService.class);

    private List<MergingTask> tasks;
    private static final String CGM_RECOGNITION_OUTPUT_NAME = "22XCORESO------S_10V1001C--00236Y_CORE-FB-100_%s-F100-%02d.xml";
    private int version;
    private String mergingDay;
    private RequestInformation requestInformation;

    @BeforeEach
    void setUp() throws IOException {
        SavedFile cgm1 = new SavedFile("mock_cgm_1.uct", getClass().getResource("/cgmResult/mock_cgm_1.uct").getPath(), "mock");
        MergingTask task1 = new MergingTask();
        Inputs inputs1 = new Inputs();
        inputs1.setTargetDate(OffsetDateTime.parse("2020-01-06T22:00Z", DateTimeFormatter.ISO_DATE_TIME));
        Outputs outputs1 = new Outputs();
        outputs1.setCgm(cgm1);
        task1.setInputs(inputs1);
        task1.setOutputs(outputs1);
        task1.setStatus(TaskStatus.SUCCESS);

        SavedFile cgm2 = new SavedFile("mock_cgm_2.uct", getClass().getResource("/cgmResult/mock_cgm_2.uct").getPath(), "mock");
        MergingTask task2 = new MergingTask();
        Inputs inputs2 = new Inputs();
        inputs2.setTargetDate(OffsetDateTime.parse("2020-01-06T23:00Z", DateTimeFormatter.ISO_DATE_TIME));
        Outputs outputs2 = new Outputs();
        outputs2.setCgm(cgm2);
        task2.setInputs(inputs2);
        task2.setOutputs(outputs2);
        task2.setStatus(TaskStatus.SUCCESS);

        tasks = Arrays.asList(task1, task2);

        dailyMergingTask = new DailyMergingTask();
        dailyMergingTask.setId(1L);
        version = 5;
        mergingDay = "20200106";
        dailyMergingTask.setVersion(version);
        Files.createDirectories(Paths.get(configuration.getDailyOutputsDirectoryPath(dailyMergingTask)));

        requestInformation = new RequestInformation("2020-01-05T23:00Z/2020-01-06T23:00Z",
                                                    "CommonGridModel",
                                                    "PRODUCTION",
                                                    "22XCORESO------S",
                                                    "62613970-472b-405a-b8c9-9e1be581f519");
    }

    @Test
    void shouldCreateCgmZip() throws IOException {
        CgmResultsService cgmResultsService = new CgmResultsService(configuration, tasksRepository, cgmRecognitionService);
        byte[] cgmRecognitionFile = Files.readAllBytes(Paths.get("src", "test", "resources", "cgmResult", "cgmRecognition_mock.xml"));
        when(cgmRecognitionService.computeCgmRecognition(requestInformation, tasks, version))
                .thenReturn(cgmRecognitionFile);
        String cgmRecognitionOutputFileName = String.format(CGM_RECOGNITION_OUTPUT_NAME, mergingDay, version);
        cgmResultsService.createCgmZip(dailyMergingTask, tasks, requestInformation);

        SavedFile cgmZip = dailyMergingTask.getDailyOutputs().getCgmZip();
        boolean cgmRecognitionFound = false;
        boolean cgmFile1Found = false;
        boolean cgmFile2Found = false;
        Enumeration<? extends ZipEntry> entries;
        try (ZipFile zipFile = new ZipFile(cgmZip.getPath())) {
            entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entryCgm = entries.nextElement();
                if (entryCgm.getName().contains("mock_cgm_1.uct")) {
                    cgmFile1Found = true;
                }
                if (entryCgm.getName().contains("mock_cgm_2.uct")) {
                    cgmFile2Found = true;
                }
                if (entryCgm.getName().contains(cgmRecognitionOutputFileName)) {
                    cgmRecognitionFound = true;
                }
            }
        }

        assertTrue(cgmFile1Found);
        assertTrue(cgmFile2Found);
        assertTrue(cgmRecognitionFound);
    }
}

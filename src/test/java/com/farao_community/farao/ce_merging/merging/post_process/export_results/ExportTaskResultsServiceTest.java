/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.export_results;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static java.io.File.separator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ExportTaskResultsServiceTest {

    @TempDir
    File tempDirectory;

    private static final String OUTPUT_PATH = "123" + separator + "outputs";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.of(2026, 7, 3, 10, 15, 0, 0, ZoneOffset.UTC);
    @Autowired
    ExportTaskResultsService exportTaskResultsService;

    @MockitoBean
    MergingTaskRepository mergingTaskRepository;

    @MockitoBean
    CeMergingConfiguration configuration;

    @Test
    void generateOutputFilesWithException() {
        final MergingTask mergingTask = initMergingTask();
        when(configuration.getOutputsDirectoryPath(any(MergingTask.class))).thenThrow(new RuntimeException("my error"));
        Throwable throwable = catchThrowable(() -> exportTaskResultsService.generateOutputFiles(mergingTask));
        assertThat(throwable).hasMessage("Results export failed for task 123 with target date 2026-07-03T10:15Z, cause: my error");
    }

    @Test
    void generateOutputFiles() throws IOException {
        when(configuration.getOutputsDirectoryPath(any(MergingTask.class))).thenReturn(tempDirectory.getPath() + separator + OUTPUT_PATH);
        final MergingTask mergingTask = initMergingTask();
        exportTaskResultsService.generateOutputFiles(mergingTask);
        verify(mergingTaskRepository, atMostOnce()).save(mergingTask);
        assertThat(mergingTask.getOutputs().getIgmQualityChecks()).hasSize(2);
        assertThat(mergingTask.getOutputs().getRealGlsk()).isNotNull();
        assertThat(FileUtils.contentEquals(mergingTask.getArtifactFile(ArtifactType.GLSK_QUALITY_REPORT), new File(mergingTask.getOutputs().getRealGlsk().getPath()))).isTrue();

        for (IgmData igmData : mergingTask.getInputs().getIgms()) {
            checkFiles(mergingTask, igmData.getCountry());
        }
    }

    private static void checkFiles(final MergingTask mergingTask, final String countryCode) throws IOException {
        final SavedFile inputSavedFileIgm = mergingTask.getInputs().getIgm(countryCode).getIgmQualityReportFile();
        final SavedFile outputSavedFile = mergingTask.getOutputs().getIgmQualityChecks().get(countryCode);
        assertThat(FileUtils.contentEquals(new File(inputSavedFileIgm.getPath()),
                                           new File(outputSavedFile.getPath()))).isTrue();
        assertThat(inputSavedFileIgm.getOriginalName()).isEqualTo(outputSavedFile.getOriginalName());
    }

    private MergingTask initMergingTask() {
        final MergingTask mergingTask = new MergingTask();
        mergingTask.setId(123L);
        mergingTask.getInputs().setTargetDate(TARGET_DATE);
        final IgmData igm1 = new IgmData();
        igm1.setCountry("FR");
        final SavedFile igmQualityReportFile1 = new SavedFile("testIgm1.txt", "src/test/resources/export_results/testIgm1.txt", "mock");
        igm1.setIgmQualityReportFile(igmQualityReportFile1);
        mergingTask.getInputs().getIgms().add(igm1);
        final IgmData igm2 = new IgmData();
        igm2.setCountry("BE");
        final SavedFile igmQualityReportFile2 = new SavedFile("testIgm2.txt", "src/test/resources/export_results/testIgm2.txt", "mock");
        igm2.setIgmQualityReportFile(igmQualityReportFile2);
        mergingTask.getInputs().getIgms().add(igm2);
        final SavedFile glskQuality = new SavedFile("qualityCheckTest.xml", "src/test/resources/export_results/qualityCheckTest.xml", "mock");
        mergingTask.setArtifact(ArtifactType.GLSK_QUALITY_REPORT, glskQuality);
        return mergingTask;
    }
}

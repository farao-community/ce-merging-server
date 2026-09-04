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
    void generateOutPutFilesWithException() {
        when(configuration.getOutputsDirectoryPath(any(MergingTask.class))).thenThrow(new RuntimeException("my error"));
        Throwable throwable = catchThrowable(() -> exportTaskResultsService.generateOutPutFiles(new MergingTask()));
        assertThat(throwable).hasMessage("Results export failed for task null with target date null, cause: my error");
    }

    @Test
    void generateOutPutFiles() throws IOException {
        when(configuration.getOutputsDirectoryPath(any(MergingTask.class))).thenReturn(tempDirectory.getPath() + separator + OUTPUT_PATH);
        final MergingTask mergingTask = initMergingTask();
        exportTaskResultsService.generateOutPutFiles(mergingTask);
        assertThat(mergingTask.getOutputs().getIgmQualityChecks()).hasSize(2);
        assertThat(mergingTask.getOutputs().getRealGlsk()).isNotNull();
        assertThat(FileUtils.contentEquals(mergingTask.getArtifactFile(ArtifactType.GLSK_QUALITY_REPORT), new File(mergingTask.getOutputs().getRealGlsk().getPath()))).isTrue();
        assertThat(FileUtils.contentEquals(new File(mergingTask.getInputs().getIgm("FR").getIgmQualityReportFile().getPath()),
                                           new File(mergingTask.getOutputs().getIgmQualityChecks().get("FR").getPath()))).isTrue();
        assertThat(FileUtils.contentEquals(new File(mergingTask.getInputs().getIgm("BE").getIgmQualityReportFile().getPath()),
                                           new File(mergingTask.getOutputs().getIgmQualityChecks().get("BE").getPath()))).isTrue();
    }

    private MergingTask initMergingTask() {
        MergingTask mergingTask = new MergingTask();
        mergingTask.setId(123L);
        mergingTask.getInputs().setTargetDate(TARGET_DATE);
        IgmData igm1 = new IgmData();
        igm1.setCountry("FR");
        igm1.setIgmQualityReportFile(new SavedFile("testIgm1.txt", "src/test/resources/export_results/testIgm1.txt", "mock"));
        mergingTask.getInputs().getIgms().add(igm1);
        IgmData igm2 = new IgmData();
        igm2.setCountry("BE");
        igm2.setIgmQualityReportFile(new SavedFile("testIgm2.txt", "src/test/resources/export_results/testIgm2.txt", "mock"));
        mergingTask.getInputs().getIgms().add(igm2);
        SavedFile glskQuality = new SavedFile("qualityCheckTest.xml", "src/test/resources/export_results/qualityCheckTest.xml", "mock");
        mergingTask.setArtifact(ArtifactType.GLSK_QUALITY_REPORT, glskQuality);
        return mergingTask;
    }
}

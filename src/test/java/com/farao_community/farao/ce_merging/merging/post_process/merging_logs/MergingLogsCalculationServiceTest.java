
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergingLogsCalculationServiceTest {

    private static final String MERGING_LOGS_DIRECTORY = "mergingLogs";
    private static final String BCI_OUTPUTS_FILENAME = "bciOutputs.json";
    private static final String CGM_NET_POSITIONS_FILENAME = "cgmNetPositions.json";
    private static final String IGMS_NET_POSITIONS_FILENAME = "igmsNetPositions.json";
    private static final String PST_OUTPUTS_FILENAME = "pstOutput.json";
    private static final String REFERENCE_PROGRAM_FILENAME = "forecastReferenceProgram.json";

    @TempDir
    Path tempDir;

    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private TsoInformationsService tsoInformationsService;

    @Mock
    private MergingTaskRepository repository;

    private MergingLogsCalculationService service;

    @BeforeEach
    void setUp() {
        service = new MergingLogsCalculationService(
                repository,
                configuration,
                tsoInformationsService
        );
    }

    @Test
    void shouldComputeAndSaveMergingLogs() throws Exception {
        final MergingTask task = createTask();

        when(tsoInformationsService.calculateTsoInformations(task)).thenReturn(List.of());
        when(configuration.getOutputsDirectoryPath(task)).thenReturn(tempDir.toString());

        service.computeMergingLogs(task);

        verify(tsoInformationsService).calculateTsoInformations(task);
        verify(repository).save(task);

        final MergingLog mergingLog = readMergingLog(task);

        assertAll(
                () -> assertEquals(
                        "2020-01-29T23:00Z/2020-01-30T23:00Z",
                        mergingLog.getReportTimeInterval().getV()
                ),
                () -> assertEquals(
                        1,
                        mergingLog.getTimeSeries().getPeriod().getInterval().size()
                )
        );

        final MergingLog.TimeSeries.Period.Interval interval = mergingLog.getTimeSeries().getPeriod().getInterval().getFirst();

        assertAll(
                () -> assertEquals("15", interval.getPos().getV()),
                () -> assertEquals(2, interval.getMergingReport().getReport().size())
        );

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = interval.getMergingReport().getReport().getFirst();

        assertAll(
                () -> assertEquals("BELGIUM", report.getName()),
                () -> assertEquals(
                        0,
                        report.getLoadFlow().getCGM().getGeneration(),
                        0.01
                ),
                () -> assertEquals(
                        307,
                        report.getLoadFlow().getCGM().getGlobalBalance()
                )
        );
    }

    private MergingLog readMergingLog(MergingTask task) throws IOException, JAXBException {
        String path = task.getOutputs().getMergingLogs().getPath();

        assertTrue(Files.exists(Path.of(path)));

        try (InputStream inputStream = Files.newInputStream(Path.of(path))) {
            Unmarshaller unmarshaller = JAXBContext
                    .newInstance(MergingLog.class)
                    .createUnmarshaller();

            return (MergingLog) unmarshaller.unmarshal(inputStream);
        }
    }

    private MergingTask createTask() {
        final MergingTask task = new MergingTask();
        task.setId(1L);
        TaskTestUtils.setTaskDefaultConfigurations(task);
        task.getInputs().setTargetDate(OffsetDateTime.parse("2020-01-30T13:00Z"));
        task.getInputs().setMergingWithInternalHvdc(false);
        addInputArtifacts(task);

        return task;
    }

    private void addInputArtifacts(MergingTask task) {
        addArtifact(task, ArtifactType.BCI_OUTPUT_FILE, BCI_OUTPUTS_FILENAME);
        addArtifact(task, ArtifactType.CGM_NET_POSITIONS_FILE, CGM_NET_POSITIONS_FILENAME);
        addArtifact(task, ArtifactType.IGMS_NET_POSITIONS_FILE, IGMS_NET_POSITIONS_FILENAME);
        addArtifact(task, ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE, REFERENCE_PROGRAM_FILENAME);
        addArtifact(task, ArtifactType.PST_OUTPUT_FILE, PST_OUTPUTS_FILENAME);
    }

    private void addArtifact(final MergingTask task, final ArtifactType type, final String fileName) {
        task.getArtifacts().putFile(type, createSavedFile(fileName));
    }

    private SavedFile createSavedFile(final String fileName) {
        final SavedFile savedFile = new SavedFile();
        savedFile.setPath(Path.of(
                "src/test/resources",
                MERGING_LOGS_DIRECTORY,
                fileName
        ).toString());

        return savedFile;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging.output.xnodes_inconsistencies;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.daily_merging.DailyMergingRepository;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import com.farao_community.farao.ce_merging.daily_merging.entities.DailyOutputs;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class XnodeResultServiceTest {

    @Mock
    private Artifacts artifacts;

    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private DailyMergingRepository repository;

    @Mock
    private DailyMergingTask dailyTask;

    @Mock
    private DailyOutputs dailyOutputs;

    @Mock
    private Inputs inputs;

    @Mock
    private MergingTask mergingTask;

    private XnodeResultService service;

    @TempDir
    private Path tempDirectory;

    @BeforeEach
    void setUp() {
        service = new XnodeResultService(configuration, repository);
    }

    @Test
    void shouldReturnFalseWhenTargetDateIsNotSecondHour() {
        assertFalse(mergingTask.isAtSecondDstHour());
    }

    @Test
    void shouldIgnoreFailedTask() throws IOException {
        final Path outputDirectory = createOutputDirectory();
        givenOutputDirectory(outputDirectory);
        when(mergingTask.getStatus()).thenReturn(TaskStatus.ERROR);
        when(dailyTask.getDailyOutputs()).thenReturn(dailyOutputs);
        service.createXnodesInconsistenciesZip(dailyTask, List.of(mergingTask));
        verify(mergingTask, never()).getArtifacts();
    }

    @Test
    void shouldCreateXnodesZipFromSuccessfulTask() throws IOException {
        final Path outputDirectory = createOutputDirectory();
        final Path xnodeFile = tempDirectory.resolve("xnodes.json");
        Files.writeString(xnodeFile, "{\"test\": true}");
        final SavedFile savedFile = new SavedFile("xnodes.json", xnodeFile.toString(), "/tasks/1/artifacts/xnodes");
        givenOutputDirectory(outputDirectory);
        givenTargetDate();
        when(dailyTask.getId()).thenReturn(1L);
        when(dailyTask.getDailyOutputs()).thenReturn(dailyOutputs);
        when(mergingTask.getStatus()).thenReturn(TaskStatus.SUCCESS);
        when(mergingTask.getArtifacts()).thenReturn(artifacts);
        when(artifacts.getFile(ArtifactType.XNODES_INCONSISTENCIES)).thenReturn(savedFile);

        service.createXnodesInconsistenciesZip(dailyTask, List.of(mergingTask));

        final Path zipFile = outputDirectory.resolve("xnodes-inconsistencies.zip");
        assertTrue(Files.exists(zipFile));
        assertTrue(Files.size(zipFile) > 0);
        verify(repository).save(dailyTask);
    }

    private void givenTargetDate() {
        when(mergingTask.getInputs()).thenReturn(inputs);
        when(inputs.getTargetDate()).thenReturn(
                OffsetDateTime.of(
                        2026,
                        9,
                        17,
                        10,
                        0,
                        0,
                        0,
                        ZoneOffset.ofHours(2)
                )
        );
    }

    private void givenOutputDirectory(final Path outputDirectory) {
        when(configuration.getDailyOutputsDirectoryPath(dailyTask))
                .thenReturn(outputDirectory.toString());
    }

    private Path createOutputDirectory() throws IOException {
        final Path outputDirectory = tempDirectory.resolve("outputs");
        Files.createDirectories(outputDirectory);
        return outputDirectory;
    }
}

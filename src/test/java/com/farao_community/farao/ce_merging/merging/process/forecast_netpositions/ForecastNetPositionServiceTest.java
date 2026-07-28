/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.forecast_netpositions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastNetPositionServiceTest {
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2020-03-15T23:00Z");
    private static final OffsetDateTime INVALID_TARGET_DATE = OffsetDateTime.parse("2030-03-15T23:00Z");
    private static final String ARTIFACTS_DIRECTORY = "artifacts";
    private static final String FORECAST_NET_POSITION_FILE = "src/test/resources/forecastNetPosition/test_npf_file.xml";

    @Mock
    private MergingTaskRepository repository;

    @Mock
    private CeMergingConfiguration configuration;

    private ForecastNetPositionService service;

    @BeforeEach
    void setUp() {
        service = new ForecastNetPositionService(repository, configuration);
    }

    @TempDir
    Path tempDir;

    @Test
    void shouldImportAndSaveForecastFile() throws IOException {
        final MergingTask task = createTask(TARGET_DATE);
        final Path artifactDirectory = tempDir.resolve(ARTIFACTS_DIRECTORY);
        Files.createDirectories(artifactDirectory);
        when(configuration.getArtifactsDirectoryPath(task)).thenReturn(artifactDirectory.toString());
        service.importForecastNetPosition(task);
        final SavedFile savedFile = task.getArtifacts().getFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE);
        assertNotNull(savedFile);
        assertTrue(Files.exists(Path.of(savedFile.getPath())));
        verify(repository).save(task);
    }

    @Test
    void shouldThrowExceptionWhenDateIsInvalid() {
        final MergingTask task = createTask(INVALID_TARGET_DATE);
        assertThrows(
                CeMergingException.class,
                () -> service.importForecastNetPosition(task)
        );

    }

    private static MergingTask createTask(OffsetDateTime targetDate) {
        final MergingTask task = new MergingTask();
        task.setId(1L);
        task.getInputs().getNetPositionForecast().setPath(FORECAST_NET_POSITION_FILE);
        task.getInputs().setTargetDate(targetDate);
        return task;
    }
}


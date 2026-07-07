/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_COUNTRY_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DKRenamingServiceTest {

    @Mock
    private CeMergingConfiguration ceMergingConfiguration;
    @Mock
    private MergingTask task;
    @Mock
    private Configurations configurations;
    @Mock
    private Artifacts artifacts;
    @InjectMocks
    private DKRenamingService service;

    @TempDir
    Path tempDir;

    @Test
    void shouldRenameAndStoreArtifact() throws Exception {
        Path tempFile = tempDir.resolve("dk-test.uct");
        Files.writeString(tempFile, "dummy-content");
        final SavedFile file = new SavedFile();
        file.setPath(tempFile.toString());
        file.setOriginalName("dk.uct");
        final IgmData dkIgm = new IgmData();
        dkIgm.setCountry(DK_COUNTRY_CODE);
        dkIgm.setIgmFile(file);
        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(dkIgm));
        inputs.setTargetDate(OffsetDateTime.of(2026, 7, 3, 10, 15, 0, 0, ZoneOffset.UTC));

        when(task.getInputs()).thenReturn(inputs);
        when(task.getArtifacts()).thenReturn(artifacts);
        when(task.getConfigurations()).thenReturn(configurations);
        when(task.getId()).thenReturn(1L);
        when(configurations.getDkHvdcXnodes()).thenReturn(List.of("XN1", "XN2"));
        when(ceMergingConfiguration.getArtifactsDirectoryPath(task)).thenReturn(tempFile.getParent().toString());
        try (MockedStatic<Network> mocked = mockStatic(Network.class)) {
            final Network network = mock(Network.class);
            mocked.when(() -> Network.read(anyString(), any())).thenReturn(network);
            service.renameDkCountry(task);
            final ArgumentCaptor<SavedFile> captor = ArgumentCaptor.forClass(SavedFile.class);
            verify(artifacts).putFile(eq(ArtifactType.DK_CONVERTED_FILE), captor.capture());
            assertTrue(captor.getValue().getOriginalName().endsWith("_DK0.uct"));
        }
    }

    @Test
    void shouldThrowExceptionWhenInputFileDoesNotExist() {
        final SavedFile file = new SavedFile();
        file.setPath("src/test/resources/does-not-exist.uct");
        file.setOriginalName("dk.uct");
        final IgmData dkIgm = new IgmData();
        dkIgm.setCountry(DK_COUNTRY_CODE);
        dkIgm.setIgmFile(file);
        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(dkIgm));
        inputs.setTargetDate(OffsetDateTime.now());

        when(task.getInputs()).thenReturn(inputs);

        final CeMergingException ex = assertThrows(CeMergingException.class, () -> service.renameDkCountry(task));
        assertThat(ex)
                .isInstanceOf(CeMergingException.class)
                .hasMessageContaining("Denmark Renaming strategy failed");
    }
}


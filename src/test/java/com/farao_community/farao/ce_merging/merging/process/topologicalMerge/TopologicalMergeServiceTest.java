/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.topologicalMerge;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;


import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;

import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class TopologicalMergeServiceTest {

    private static final String GERMAN_PRE_MERGE_FILE_NAME = "german-pre-merge.uct";
    private static final String FR_IGM_FILE_NAME = "fr.uct";
    private static final String DK_CONVERTED_FILE_NAME = "dk-converted.uct";
    private static final String COUNTRY_FR = "FR";
    private static final String COUNTRY_BE = "BE";
    private static final String COUNTRY_IT = "IT";
    private static final String IT_PRETREATED_FILE_NAME = "it-pretreated.uct";
    private static final String BE_IGM_FILENAME = "be.uct";
    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private MergingTask task;

    @Mock
    private Network network1;

    @Mock
    private Network network2;

    @Mock
    private Network germanNetwork;

    @Mock
    private Network danishNetwork;

    @Mock
    private Network preTreatedNetwork;

    @Mock
    private Network mergedNetwork;

    @Mock
    private Inputs inputs;

    @Mock
    private Artifacts artifacts;

    @TempDir
    Path tempDirectory;

    private TopologicalMergeService service;

    @BeforeEach
    void setUp() {
        service = new TopologicalMergeService(configuration);
    }

    @Test
    void shouldMergeNetworksAndSaveTopologicalMergeArtifact() throws Exception {
        final Path frIgmPath = createFile(FR_IGM_FILE_NAME);
        final Path beIgmPath = createFile(BE_IGM_FILENAME);
        final Path germanPremergeFilePath = createFile(GERMAN_PRE_MERGE_FILE_NAME);
        final Path dkConvertedFilePath = createFile(DK_CONVERTED_FILE_NAME);
        final Path itPreTreatedPath = createFile(IT_PRETREATED_FILE_NAME);
        final IgmData frIgm = createIgm(COUNTRY_FR, frIgmPath);
        final IgmData beIgm = createIgm(COUNTRY_BE, beIgmPath);

        createTask(
                List.of(frIgm, beIgm),
                Map.of(COUNTRY_IT, savedFile(itPreTreatedPath)),
                germanPremergeFilePath,
                dkConvertedFilePath
        );

        try (MockedStatic<Network> networkMock = mockStatic(Network.class);
             MockedStatic<FileStorageUtils> fileStorageMock = mockStatic(FileStorageUtils.class)) {

            networkMock.when(() -> Network.read(
                    anyString(),
                    any(InputStream.class),
                    any(),
                    any(),
                    any(Properties.class)
            )).thenReturn(
                    network1,
                    network2,
                    germanNetwork,
                    danishNetwork,
                    preTreatedNetwork
            );

            networkMock.when(() -> Network.merge(
                    anyString(),
                    any(Network[].class)
            )).thenReturn(mergedNetwork);

            // When
            service.mergeInitialIgms(task);

            // Then
            ArgumentCaptor<Network[]> networksCaptor = ArgumentCaptor.forClass(Network[].class);

            networkMock.verify(() -> Network.merge(
                    anyString(),
                    networksCaptor.capture()
            ));

            assertThat(networksCaptor.getValue())
                    .containsExactly(
                            network1,
                            network2,
                            germanNetwork,
                            danishNetwork,
                            preTreatedNetwork
                );

            fileStorageMock.verify(() ->
                    FileStorageUtils.saveArtifactNetwork(
                            ArtifactType.TOPOLOGICAL_MERGE_FILE,
                            mergedNetwork,
                            task,
                            UCTE_FORMAT,
                            configuration
                    )
            );
        }
    }

    @Test
    void shouldWrapExceptionWhenNetworkCannotBeRead() throws Exception {
        final Path frPath = createFile(FR_IGM_FILE_NAME);
        final Path germanPath = createFile(GERMAN_PRE_MERGE_FILE_NAME);
        final Path danishPath = createFile(DK_CONVERTED_FILE_NAME);
        when(task.getId()).thenReturn(42L);
        final IgmData frIgm = createIgm(COUNTRY_FR, frPath);
        createTask(List.of(frIgm), Map.of(), germanPath, danishPath);
        try (MockedStatic<Network> networkMock = mockStatic(Network.class)) {
            networkMock.when(() -> Network.read(
                    anyString(),
                    any(InputStream.class),
                    any(),
                    any(),
                    any(Properties.class)
            )).thenThrow(new RuntimeException("Invalid UCTE network"));
            assertThatThrownBy(() -> service.mergeInitialIgms(task))
                    .isInstanceOf(CeMergingException.class)
                    .hasMessageContaining("Topological merge failed")
                    .hasMessageContaining("task 42")
                    .hasRootCauseMessage("Invalid UCTE network");

            networkMock.verify(() -> Network.merge(
                    anyString(),
                    any(Network[].class)
            ), never());
        }
    }

    private void createTask(final List<IgmData> igms, final Map<String, SavedFile> preTreatedIgms, final Path germanPath, final Path danishPath) {
        when(task.getInputs()).thenReturn(inputs);
        when(task.getArtifacts()).thenReturn(artifacts);
        when(task.getTargetDate()).thenReturn(BEGINNING_OF_2000);
        when(inputs.getIgms()).thenReturn(igms);
        when(task.getArtifactPath(ArtifactType.GERMAN_PRE_MERGED_IGM)).thenReturn(germanPath.toString());
        when(task.getArtifactPath(ArtifactType.DK_CONVERTED_FILE)).thenReturn(danishPath.toString());
        when(task.hasPreTreatedIgm(anyString())).thenAnswer(invocation -> preTreatedIgms.containsKey(invocation.getArgument(0)));
        when(artifacts.getPreTreatedIgmMap()).thenReturn(preTreatedIgms);
    }

    private Path createFile(final String fileName) throws Exception {
        Path path = tempDirectory.resolve(fileName);
        Files.write(path, new byte[]{1});
        return path;
    }

    private SavedFile savedFile(final Path path) {
        return new SavedFile(
                path.getFileName().toString(),
                path.toString(),
                "/tasks/artifacts/test"
        );
    }

    private IgmData createIgm(final String country, final Path path) {
        final IgmData igm = new IgmData();
        igm.setCountry(country);
        igm.setIgmFile(
                new SavedFile(
                        path.getFileName().toString(),
                        path.toString(),
                        "/tasks/inputs/" + country
                )
        );
        return igm;
    }
}

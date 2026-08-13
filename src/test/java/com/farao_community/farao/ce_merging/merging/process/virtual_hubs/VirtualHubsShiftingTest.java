/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.virtual_hubs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.SavedFile;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.model.hourly.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.process.virtuals_hubs.VirtualHubsShifting;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VirtualHubsShiftingTest {
    private static final String RESOURCES_PATH = "src/test/resources/virtual_hubs/";
    private static final String NODE_NAME = "XAA_AA1A";
    private static final long TASK_ID_1 = 1L;
    private static final long TASK_ID_2 = 2L;
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2026-07-29T23:30Z");

    @TempDir
    Path tempDir;

    private CeMergingConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = mock(CeMergingConfiguration.class);
        when(configuration.getArtifactsDirectoryPath(any())).thenReturn(tempDir.toString());
    }

    @Test
    void applyVirtualHubFlowsShouldNotShiftDisconnectedVirtualHub() throws FileNotFoundException {
        final MergingTask task = createTask(TASK_ID_1, "network.uct");
        final Network initialNetwork = readNetwork(task);
        final DanglingLine initialDanglingLine = getDanglingLine(initialNetwork);
        assertFalse(initialDanglingLine.getTerminal().isConnected());
        assertEquals(0.0, initialDanglingLine.getP0());
        final Map<String, Double> virtualHubGaps = VirtualHubsShifting.applyVirtualHubFlows(task, configuration);
        final Network finalNetwork = readNetwork(task);
        final DanglingLine finalDanglingLine = getDanglingLine(finalNetwork);
        assertTrue(virtualHubGaps.isEmpty());
        assertEquals(0.0, finalDanglingLine.getP0());
    }

    @Test
    void applyVirtualHubFlowsShouldShiftConnectedVirtualHub() throws FileNotFoundException {
        final MergingTask task = createTask(TASK_ID_2, "network_terminal_connected.uct");
        final Network initialNetwork = readNetwork(task);
        final DanglingLine initialDanglingLine = getDanglingLine(initialNetwork);
        assertTrue(initialDanglingLine.getTerminal().isConnected());
        assertEquals(0.0, initialDanglingLine.getP0());

        final Map<String, Double> virtualHubGaps = VirtualHubsShifting.applyVirtualHubFlows(task, configuration);
        final Network finalNetwork = readNetwork(task);
        final DanglingLine finalDanglingLine = getDanglingLine(finalNetwork);
        assertFalse(virtualHubGaps.isEmpty());
        assertEquals(Map.of("DE", 51.0), virtualHubGaps);
        assertEquals(51.0, finalDanglingLine.getP0());
    }

    private DanglingLine getDanglingLine(final Network network) {
        return network.getDanglingLineStream()
                .filter(dl -> NODE_NAME.equals(dl.getPairingKey()))
                .findFirst()
                .orElseThrow();
    }

    private MergingTask createTask(final long taskId, final String networkFile) {
        final MergingTask task = new MergingTask();
        task.setId(taskId);
        task.getInputs().setTargetDate(TARGET_DATE);

        task.getConfigurations().setVirtualHubList(List.of(
                new VirtualHubRecord("CODE", "EICODE_01", NODE_NAME, "DE", "EICODE_02")
        ));
        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(
                ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE,
                createSavedFile("forecastReferenceProgram.json")
        );
        artifacts.putFile(
                ArtifactType.TGM_FILE_AFTER_RECESSIVITY,
                createSavedFile(networkFile)
        );
        task.setArtifacts(artifacts);
        return task;
    }

    private Network readNetwork(final MergingTask task) {
        return Network.read(task.getArtifacts().getFile(ArtifactType.TGM_FILE_AFTER_RECESSIVITY).getPath());
    }

    private static SavedFile createSavedFile(final String fileName) {
        return new SavedFile(fileName, RESOURCES_PATH + fileName, "mock");
    }
}



/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.alegro;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class AlegroServiceTest {

    private static final double ASSERTION_DELTA = 1e-6;
    private static final int ALEGRO_FLOW_THRESHOLD_0 = 0;
    private static final int ALEGRO_FLOW_THRESHOLD_5 = 5;
    private static final int ALEGRO_FLOW_THRESHOLD_10 = 10;

    @TempDir
    Path tempDirectory;

    @Autowired
    private AlegroService alegroService;

    @Test
    void shouldThrowExceptionWhenTwoFlowsHaveTheSameSignAndLoadsNotBelowThreshold() {
        assertThrows(
                CeMergingException.class,
                () -> alegroService.checkFlowDirection(200, 210, ALEGRO_FLOW_THRESHOLD_0)
        );
    }

    @Test
    void shouldNotThrowExceptionWhenTwoFlowsHaveTheSameSignAndLoadsBelowThreshold() {
        assertDoesNotThrow(
                () -> alegroService.checkFlowDirection(5, 5, ALEGRO_FLOW_THRESHOLD_5)
        );
    }

    @Test
    void shouldThrowExceptionWhenTheDifferenceBetweenFlowsExceedsTheThreshold() {
        assertThrows(
                CeMergingException.class,
                () -> alegroService.checkFlowCompliance(200, 210, ALEGRO_FLOW_THRESHOLD_5)
        );
    }

    @Test
    void shouldThrowExceptionWhenTheGapNpfInitialFlowExceedsTheThreshold() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        assertThrows(
                CeMergingException.class,
                () -> alegroService.getAlegroNetPositions(referenceProgram, false, 200, -200, ALEGRO_FLOW_THRESHOLD_5)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, true",
        "true, false, true",
        "false, true, true",
        "true, true, false"
    })
    void shouldDetectAlegroOutageBasedOnXNodesConnection(final boolean albeConnected, final boolean aldeConnected, final boolean expectedOutage) {
        final DanglingLine albe = mock(DanglingLine.class);
        final DanglingLine alde = mock(DanglingLine.class);

        final Terminal albeTerminal = mock(Terminal.class);
        final Terminal aldeTerminal = mock(Terminal.class);

        when(albe.getTerminal()).thenReturn(albeTerminal);
        when(alde.getTerminal()).thenReturn(aldeTerminal);

        when(albeTerminal.isConnected()).thenReturn(albeConnected);
        when(aldeTerminal.isConnected()).thenReturn(aldeConnected);

        assertEquals(expectedOutage, alegroService.isAlegroInOutage(List.of(albe, alde)));
    }

    @Test
    void shouldGetCorrectAlegroNetPositions() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        final AlegroData alegroData = alegroService.getAlegroNetPositions(referenceProgram, false, -200, 200, ALEGRO_FLOW_THRESHOLD_10);
        assertEquals(200, alegroData.aldeFlows().initialFlow(), ASSERTION_DELTA);
        assertEquals(210, alegroData.aldeFlows().targetFlow(), ASSERTION_DELTA);
        assertEquals(10, alegroData.aldeFlows().gapNpfInitialFlow(), ASSERTION_DELTA);
        assertEquals(-200, alegroData.albeFlows().initialFlow(), ASSERTION_DELTA);
        assertEquals(-210, alegroData.albeFlows().targetFlow(), ASSERTION_DELTA);
        assertEquals(-10, alegroData.albeFlows().gapNpfInitialFlow(), ASSERTION_DELTA);
    }

    @Test
    void shouldNotCheckFlowGapWhenAlegroIsInOutage() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        assertDoesNotThrow(
                () ->
                alegroService.getAlegroNetPositions(referenceProgram, true, 1000, -1000, ALEGRO_FLOW_THRESHOLD_5)
        );
    }

    @Test
    void shouldLimitAlegroP0ToCommonEcLimit() {
        // ALDE target flow = 200 MW, ALBE target flow = -200 MW
        //EC limit = [-100 MW, +100 MW]
        final MergingTask task = new MergingTask();
        task.setArtifact(BCI_OUTPUT_FILE, copyResource("bciOutputs.json"));
        task.setArtifact(TGM_FILE_AFTER_RECESSIVITY, copyResource("network_alegro.uct"));
        task.setArtifact(ALEGRO_NET_POSITIONS, copyResource("alegroNetPositions.json"));
        alegroService.updateAlegroP0(task);
        final Network network = Network.read(task.getArtifactPath(TGM_FILE_AFTER_RECESSIVITY));
        assertEquals(-100., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME), ASSERTION_DELTA);
        assertEquals(100., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME), ASSERTION_DELTA);

    }

    @Test
    void shouldSetAlegroP0ToZeroWhenInOutage() {
        final SavedFile tgmFile = copyResource("network_alegro_outage.uct");
        final Network network = Network.read(tgmFile.getPath());
        final List<DanglingLine> alegroDanglingLines = network.getDanglingLineStream()
                .filter(dl -> VIRTUAL_HUB_ALEGRO_BE_NODE_NAME.equals(dl.getPairingKey()) || VIRTUAL_HUB_ALEGRO_DE_NODE_NAME.equals(dl.getPairingKey()))
                .toList();
        assertEquals(-100., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME), ASSERTION_DELTA);
        assertEquals(100., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME), ASSERTION_DELTA);

        alegroService.correctOutage(network, alegroDanglingLines, tgmFile.getPath());

        assertEquals(0., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME), ASSERTION_DELTA);
        assertEquals(0., getAlegroP0(network, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME), ASSERTION_DELTA);
    }

    private SavedFile copyResource(final String fileName) {
        final String resourcePath = "/alegro/" + fileName;

        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            final Path destination = tempDirectory.resolve(fileName);
            Files.copy(inputStream, destination);
            return new SavedFile(fileName, destination.toString(), "test");
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Unable to copy test resource: " + resourcePath, e);
        }
    }

    private double getAlegroP0(final Network network, final String pairingKey) {
        return network.getDanglingLineStream()
                .filter(danglingLine -> pairingKey.equals(danglingLine.getPairingKey()))
                .findFirst()
                .orElseThrow(() -> new CeMergingException(
                        "No dangling line found with pairing key " + pairingKey))
                .getP0();
    }

    private ReferenceProgram getReferenceProgram() {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/alegro/forecastReferenceProgram.json")) {
            return JsonUtils.read(ReferenceProgram.class, inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}

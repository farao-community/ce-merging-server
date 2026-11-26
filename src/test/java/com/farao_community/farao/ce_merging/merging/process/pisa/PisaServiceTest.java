/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.pisa;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsValues;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import test_utils.assertions.NetworkAssert;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PISA_RENAMING;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.Country.IT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.createMockTask;
import static test_utils.CeTestUtils.mockBoundaryLine;
import static test_utils.CeTestUtils.mockNetPositions;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.withArtifact;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class PisaServiceTest {

    private final CeMergingConfiguration configuration = mock(CeMergingConfiguration.class);

    private static final String FR_PISA1 = "XFG.IL12";
    private static final String FR_PISA2 = "XFG.IL14";
    private static final String IT_PISA1 = "XFG.IL11";
    private static final String IT_PISA2 = "XFG.IL13";
    private static final String IT_FR_OLD1 = "XPI_GI11";
    private static final String IT_FR_OLD2 = "XPI_GI12";

    @TempDir
    private File tempDirectory;

    @Test
    void shouldDisplaceNodes() throws FileNotFoundException {
        final MergingTask task = getPisaTask("network_ok.uct");
        when(configuration.getArtifactsDirectoryPath(any(MergingTask.class))).thenReturn(tempDirectory.getPath());
        final PisaService service = new PisaService(configuration);
        service.displacePisaNodes(task);

        final Network cgm = task.getArtifact(CGM_FILE_AFTER_PISA_RENAMING, Network.class);

        NetworkAssert.assertThat(cgm)
                .hasBoundaryLine("XFG.IL11 F.TEST11 1")
                .hasLine("IPI_GI11 I.TEST11 1")
                .hasBoundaryLine("IPI_GI11 XFG.IL13 1")
                .hasBoundaryLine("XFG.IL12 IPI_GI12 1")
                .hasLine("I.TEST11 IPI_GI12 1");
    }

    @Test
    void shouldThrowExceptionIfInvalidLineConnected() {
        final MergingTask task = getPisaTask("network_ko.uct");
        final PisaService service = new PisaService(configuration);
        assertThatThrownBy(() -> service.displacePisaNodes(task))
                .isValidServiceException()
                .hasMessage("PiSa post-treatment: cannot remove line FFG.IL11 FFG.IL12 1 because it is in operation.");
    }

    @Test
    void shouldRemovePiSaFromItalianNps() {
        final MergingTask task = mock(MergingTask.class);
        final Network mockItaPisa = mockPisaBorder(IT, -20, -20);
        final Network mockFrPisa = mockPisaBorder(FR, 30, 30);
        when(task.getPreTreatedOrInputIgm(IT)).thenReturn(mockItaPisa);
        when(task.getPreTreatedOrInputIgm(FR)).thenReturn(mockFrPisa);

        final NetPositions itaNp = mockNetPositions(100,
                                                    90,
                                                    80,
                                                    Map.of(IT_FR_OLD1, 1.,
                                                           IT_FR_OLD2, 2.));
        final NetPositions frNp = mockNetPositions(200,
                                                   190,
                                                   15,
                                                   Map.of(IT_FR_OLD1, 4.,
                                                          IT_FR_OLD2, 5.));

        PisaService.fixPisaNetPositions(task, new NetPositionsResults(Map.of(FR.name(), frNp,
                                                                             IT.name(), itaNp)));

        final NetPositionsValues itaGlobalNp = itaNp.getGlobalNetPosition();

        // shifted by PiSa nodes
        assertThat(itaGlobalNp.getWithVirtualHubs()).isEqualTo(200); // 100 + 2*30 - 2*-20
        assertThat(itaGlobalNp.getWithoutVirtualHubs()).isEqualTo(90); // unchanged
        assertThat(itaNp.getOutBciNetPosition()).isEqualTo(180); // 80 + 2*30 - 2*-20
        assertThat(itaNp.getVirtualHubsExchanges())
                .containsEntry(IT_PISA1, -30.)
                .containsEntry(IT_PISA2, -30.)
                .doesNotContainKeys(IT_FR_OLD1, IT_FR_OLD2);

        final NetPositionsValues frGlobalNp = frNp.getGlobalNetPosition();

        assertThat(frGlobalNp.getWithVirtualHubs()).isEqualTo(200); // unchanged
        assertThat(frGlobalNp.getWithoutVirtualHubs()).isEqualTo(190); // unchanged
        assertThat(frNp.getOutBciNetPosition()).isEqualTo(15); // unchanged
        assertThat(frNp.getVirtualHubsExchanges())
                .containsEntry(FR_PISA1, 30.)
                .containsEntry(FR_PISA2, 30.)
                .doesNotContainKeys(IT_FR_OLD1, IT_FR_OLD2);
    }

    private Network mockPisaBorder(final Country country,
                                   final double pisa1P0,
                                   final double pisa2P0) {
        final Network network = mock(Network.class);
        final BoundaryLine xpiGi11 = mockBoundaryLine(IT_FR_OLD1, pisa1P0, country);
        final BoundaryLine xpiGi12 = mockBoundaryLine(IT_FR_OLD2, pisa2P0, country);
        when(network.getBoundaryLineStream()).thenAnswer(invocation -> Stream.of(xpiGi11, xpiGi12));
        return network;
    }

    private MergingTask getPisaTask(final String networkName) {
        return createMockTask(1L, new Inputs(), withArtifact(TOPOLOGICAL_MERGE_FILE, networkName, stringPathOf("pisa") + "/"));
    }
}

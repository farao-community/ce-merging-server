/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.netpositions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsValues;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.powsybl.iidm.network.Country.AT;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.CZ;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;
import static com.powsybl.iidm.network.Country.ES;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.Country.HR;
import static com.powsybl.iidm.network.Country.NL;
import static com.powsybl.iidm.network.Country.PL;
import static com.powsybl.iidm.network.Country.RO;
import static com.powsybl.iidm.network.Country.SI;
import static com.powsybl.iidm.network.Country.SK;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test_utils.CeTestUtils.assertSumEquals;
import static test_utils.CeTestUtils.getIgm;
import static test_utils.CeTestUtils.getTestConfiguration;
import static test_utils.CeTestUtils.mockMergingTaskRepository;
import static test_utils.CeTestUtils.stringPathOf;

class NetPositionServiceTest {

    private static final Percentage PERCENT_TOLERANCE = Percentage.withPercentage(1);
    private static final double VALUE_TOLERANCE = .1;

    private final CeMergingConfiguration configuration = getTestConfiguration();
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier = () -> new LoadFlow.Runner(new OpenLoadFlowProvider());

    private final NetPositionService service = new NetPositionService(mockMergingTaskRepository(), configuration, loadFlowRunnerSupplier);

    @Test
    void computeInitialNetPositionsTest() throws IOException {

        final List<IgmData> igms = Stream.of("20190618_0030_2D2_FR0.uct",
                                             "20190618_0030_2D2_AT1.uct",
                                             "20190618_0030_2D2_BE0.UCT",
                                             "20190618_0030_2D2_CZ0.UCT",
                                             "20190618_0030_2D2_HR1.uct",
                                             "20190618_0030_2D2_HU0.uct",
                                             "20190618_0030_2D2_NL0.uct",
                                             "20190618_0030_2D2_PL0.uct",
                                             "20190618_0030_2D2_RO0.UCT",
                                             "20190618_0030_2D2_SI0.UCT",
                                             "20190618_0030_2D2_SK0.UCT",
                                             "20190617_0030_FO1_ES0.UCT")
                .map(fileName -> getIgm("netPositionsData/" + fileName))
                .toList();

        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(DK_CONVERTED_FILE,
                          new SavedFile("denmarkRenamedIgmFile.uct",
                                        stringPathOf("netPositionsData/denmarkRenamedIgmFile.uct"),
                                        "mock"));
        artifacts.putFile(GERMAN_PRE_MERGED_IGM,
                          new SavedFile("germanPreMergedIgmFile.uct",
                                        stringPathOf("netPositionsData/germanPreMergedIgmFile.uct"),
                                        "mock"));

        final Inputs inputs = new Inputs();
        inputs.setTargetDate(OffsetDateTime.of(2019, 6, 18,
                                               1, 30, 0, 0, UTC));
        inputs.setIgms(igms);

        final MergingTask task = new MergingTask();
        task.setId(1L);
        task.setInputs(inputs);
        task.setArtifacts(artifacts);
        task.setConfigurations(new Configurations());
        TaskTestUtils.setTaskDefaultConfigurations(task);
        final String loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        TaskTestUtils.setLoadflowParameters(task, loadflowParametersFile);

        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        service.computeInitialNetPositions(task);

        final NetPositionsResults netPositionsFile = task.getArtifact(IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);
        testNetPositionsValues(netPositionsFile);
        testDetailedExchanges(netPositionsFile);
        testOutBciNetPositions(netPositionsFile);
        testGenerationAndLoad(netPositionsFile);

    }

    private void testNetPositionsValues(final NetPositionsResults netPositions) {
        assertAllPositionsEqual(netPositions.get(AT), 510);
        assertAllPositionsEqual(netPositions.get(FR), 1200);
        assertAllPositionsEqual(netPositions.get(BE), 860);
        assertAllPositionsEqual(netPositions.get(CZ), 1230);
        assertAllPositionsEqual(netPositions.get(HR), 20);
        assertAllPositionsEqual(netPositions.get(NL), 810);
        assertAllPositionsEqual(netPositions.get(PL), 120);
        assertAllPositionsEqual(netPositions.get(RO), 20);
        assertAllPositionsEqual(netPositions.get(SI), 450);
        assertAllPositionsEqual(netPositions.get(SK), -110);
        assertAllPositionsEqual(netPositions.get(DE), 200);

        final NetPositions danishNetPositions = netPositions.get(DK);
        assertNotNull(danishNetPositions);

        final SoftAssertions soft = new SoftAssertions();
        soft.assertThat(danishNetPositions.getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(20., PERCENT_TOLERANCE);

        Stream.of(danishNetPositions.getGlobalNetPosition().getWithoutVirtualHubs(),
                  danishNetPositions.getInRegionNetPosition().getWithVirtualHubs(),
                  danishNetPositions.getInRegionNetPosition().getWithoutVirtualHubs())
                .map(soft::assertThat)
                .forEach(d -> d.isCloseTo(0, PERCENT_TOLERANCE));

        soft.assertAll();
    }

    private void testDetailedExchanges(final NetPositionsResults netPositions) {
        final NetPositions frenchNetPositions = netPositions.get(FR);
        assertNotNull(frenchNetPositions);
        Map<String, Double> globalDetailedExchanges = frenchNetPositions.getGlobalDetailedExchanges();
        assertEquals(3, globalDetailedExchanges.size());
        assertSumEquals(1200, globalDetailedExchanges);
        assertSumEquals(0, frenchNetPositions.getVirtualHubsExchanges());

        final NetPositions slovakianNetPositions = netPositions.get(SK);
        assertNotNull(slovakianNetPositions);
        globalDetailedExchanges = slovakianNetPositions.getGlobalDetailedExchanges();
        assertEquals(1, globalDetailedExchanges.size());
        assertSumEquals(-110.00, globalDetailedExchanges);

    }

    private void testOutBciNetPositions(final NetPositionsResults netPositions) {
        final NetPositions spanishNetPositions = netPositions.get(ES);
        assertNotNull(spanishNetPositions);

        assertSumEquals(spanishNetPositions.getGlobalNetPosition().getWithVirtualHubs()
                        - spanishNetPositions.getGlobalNetPosition().getWithoutVirtualHubs(),
                        spanishNetPositions.getVirtualHubsExchanges());

        assertSumEquals(spanishNetPositions.getGlobalNetPosition().getWithVirtualHubs(),
                        spanishNetPositions.getGlobalDetailedExchanges());

        assertEquals(20, spanishNetPositions.getGlobalDetailedExchanges().get("MA"), VALUE_TOLERANCE);
        assertEquals(20, spanishNetPositions.getOutBciNetPosition(), VALUE_TOLERANCE);

    }

    private void testGenerationAndLoad(NetPositionsResults netPositionsFile) {
        assertEquals(-10, Objects.requireNonNull(netPositionsFile.get(AT)).getGenerationAndLoadQuantity().generation(), VALUE_TOLERANCE);
        assertEquals(1000, Objects.requireNonNull(netPositionsFile.get(AT)).getGenerationAndLoadQuantity().load(), VALUE_TOLERANCE);
    }

    private void assertAllPositionsEqual(final NetPositions netPositions,
                                         final double expected) {
        assertNotNull(netPositions);
        final NetPositionsValues global = netPositions.getGlobalNetPosition();
        final NetPositionsValues inRegion = netPositions.getInRegionNetPosition();
        final SoftAssertions soft = new SoftAssertions();

        Stream.of(global.getWithVirtualHubs(),
                  global.getWithoutVirtualHubs(),
                  inRegion.getWithVirtualHubs(),
                  inRegion.getWithoutVirtualHubs())
                .map(soft::assertThat)
                .forEach(d -> d.isCloseTo(expected, PERCENT_TOLERANCE));

        soft.assertAll();
    }
}

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
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.loadflow.LoadFlow;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.CeTestUtils.anyTask;
import static test_utils.CeTestUtils.stringPathOf;

@SpringBootTest
class NetPositionServiceTest {

    private static final Percentage VALUE_TOLERANCE = Percentage.withPercentage(1);
    private SavedFile igmDeFile;
    private SavedFile igmDkFile;
    private IgmData igmFr;
    private IgmData igmAt;
    private IgmData igmBe;
    private IgmData igmCz;
    private IgmData igmHr;
    private IgmData igmHu;
    private IgmData igmNl;
    private IgmData igmPl;
    private IgmData igmRo;
    private IgmData igmSi;
    private IgmData igmSk;
    private IgmData igmEs;

    @Autowired
    private CeMergingConfiguration configuration;
    @Autowired
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    private NetPositionService netPositionService;
    private String loadflowParametersFile;

    @BeforeEach
    void setUp() {
        igmFr = new IgmData();
        igmFr.setCountry("FR");
        igmFr.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_FR0.uct"));

        igmAt = new IgmData();
        igmAt.setCountry("AT");
        igmAt.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_AT1.uct"));

        igmBe = new IgmData();
        igmBe.setCountry("BE");
        igmBe.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_BE0.UCT"));

        igmCz = new IgmData();
        igmCz.setCountry("CZ");
        igmCz.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_CZ0.UCT"));

        igmHr = new IgmData();
        igmHr.setCountry("HR");
        igmHr.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_HR1.uct"));

        igmHu = new IgmData();
        igmHu.setCountry("HU");
        igmHu.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_HU0.uct"));

        igmNl = new IgmData();
        igmNl.setCountry("NL");
        igmNl.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_NL0.uct"));

        igmPl = new IgmData();
        igmPl.setCountry("PL");
        igmPl.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_PL0.uct"));

        igmRo = new IgmData();
        igmRo.setCountry("RO");
        igmRo.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_RO0.UCT"));

        igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmSi.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_SI0.UCT"));

        igmSk = new IgmData();
        igmSk.setCountry("SK");
        igmSk.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_SK0.UCT"));

        igmEs = new IgmData();
        igmEs.setCountry("ES");
        igmEs.setIgmFilePath(stringPathOf("netPositionsData/20190617_0030_FO1_ES0.UCT"));

        igmDeFile = new SavedFile("germanPreMergedIgmFile.uct", stringPathOf("netPositionsData/germanPreMergedIgmFile.uct"), "mock");
        igmDkFile = new SavedFile("denmarkRenamedIgmFile.uct", stringPathOf("netPositionsData/denmarkRenamedIgmFile.uct"), "mock");

        loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        final MergingTaskRepository repository = mock(MergingTaskRepository.class);
        when(repository.save(anyTask())).thenAnswer(invocation -> invocation.getArgument(0));
        netPositionService = new NetPositionService(repository, configuration, loadFlowRunnerSupplier);
    }

    @Test
    void computeInitialNetPositionsTest() throws IOException {

        List<IgmData> igms = Arrays.asList(igmFr, igmBe, igmAt, igmCz, igmHr, igmHu, igmNl, igmPl, igmRo, igmSi, igmSk, igmEs);

        Artifacts artifacts = new Artifacts();
        artifacts.putFile(DK_CONVERTED_FILE, igmDkFile);
        artifacts.putFile(GERMAN_PRE_MERGED_IGM, igmDeFile);

        Inputs inputs = new Inputs();
        inputs.setTargetDate(BEGINNING_OF_2000);
        inputs.setIgms(igms);

        Configurations configurations = new Configurations();

        final MergingTask task = new MergingTask();
        task.setId(1L);
        task.setInputs(inputs);
        task.setArtifacts(artifacts);
        task.setConfigurations(configurations);
        TaskTestUtils.setTaskDefaultConfigurations(task);
        TaskTestUtils.setLoadflowParameters(task, loadflowParametersFile);

        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        netPositionService.computeInitialNetPositions(task);

        NetPositionsResults netPositionsFile = task.getArtifact(IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);
        testNetPositionsValues(netPositionsFile);
        testDetailedExchanges(netPositionsFile);
        testOutBciNetPositions(netPositionsFile);
        testGenerationAndLoad(netPositionsFile);

    }

    private void testNetPositionsValues(NetPositionsResults netPositions) {
        final SoftAssertions soft = new SoftAssertions();
        soft.assertThat(netPositions.get(AT).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(510, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(510, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(510, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(510, VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(FR).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(1200, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(FR).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(1200, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(FR).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(1200, VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(FR).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(1200, VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(BE).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(860., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(BE).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(860., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(BE).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(860., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(BE).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(860., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(CZ).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(1230., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(CZ).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(1230., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(CZ).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(1230., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(CZ).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(1230., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(HR).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(HR).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(HR).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(HR).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(AT).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(510., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(510., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(510., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(AT).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(510., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(NL).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(810., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(NL).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(810., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(NL).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(810., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(NL).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(810., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(PL).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(120., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(PL).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(120., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(PL).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(120., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(PL).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(120., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(RO).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(RO).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(RO).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(RO).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(SI).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(450., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SI).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(450., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SI).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(450., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SI).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(450., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(SK).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(-110., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SK).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(-110., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SK).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(-110., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(SK).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(-110., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(DE).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(200., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DE).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(200., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DE).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(200., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DE).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(200., VALUE_TOLERANCE);

        soft.assertThat(netPositions.get(DK).getGlobalNetPosition().getWithVirtualHubs())
                .isCloseTo(20., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DK).getGlobalNetPosition().getWithoutVirtualHubs())
                .isCloseTo(0., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DK).getInRegionNetPosition().getWithVirtualHubs())
                .isCloseTo(0., VALUE_TOLERANCE);
        soft.assertThat(netPositions.get(DK).getInRegionNetPosition().getWithoutVirtualHubs())
                .isCloseTo(0., VALUE_TOLERANCE);

        soft.assertAll();
    }

    private void testDetailedExchanges(NetPositionsResults netPositionsFile) {
        NetPositions netPositionsFR = netPositionsFile.get(FR);
        Map<String, Double> globalDetailedExchanges = netPositionsFR.getGlobalDetailedExchanges();
        assertEquals(3, globalDetailedExchanges.size());
        assertEquals(1200, globalDetailedExchanges.values().stream().mapToDouble(v -> v).sum(), 0.1);

        assertEquals(0, netPositionsFR.getVirtualHubsExchanges().values().stream().mapToDouble(v -> v).sum(), 0.1);

        NetPositions netPositionsSK = netPositionsFile.get(SK);
        globalDetailedExchanges = netPositionsSK.getGlobalDetailedExchanges();
        assertEquals(1, globalDetailedExchanges.size());
        assertEquals(-110.00, globalDetailedExchanges.values().stream().mapToDouble(v -> v).sum(), 0.1);

    }

    private void testOutBciNetPositions(NetPositionsResults netPositionsFile) {
        NetPositions netPositionsEs = netPositionsFile.get(ES);
        double sumVirtualHubs = netPositionsEs.getVirtualHubsExchanges().values().stream().mapToDouble(v -> v).sum();
        assertEquals(sumVirtualHubs, netPositionsEs.getGlobalNetPosition().getWithVirtualHubs() - netPositionsEs.getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        double sumExchanges = netPositionsEs.getGlobalDetailedExchanges().values().stream().mapToDouble(v -> v).sum();
        assertEquals(sumExchanges, netPositionsEs.getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(20, netPositionsEs.getGlobalDetailedExchanges().get("MA"), 0.1);
        assertEquals(20, netPositionsEs.getOutBciNetPosition(), 0.1);

    }

    private void testGenerationAndLoad(NetPositionsResults netPositionsFile) {
        assertEquals(-10, netPositionsFile.get(AT).getGenerationAndLoadQuantity().generation(), 0.1);
        assertEquals(1000, netPositionsFile.get(AT).getGenerationAndLoadQuantity().load(), 0.1);
    }
}

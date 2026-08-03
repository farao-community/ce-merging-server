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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static com.powsybl.iidm.network.Country.HU;
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

@SpringBootTest
public class NetPositionServiceTest {
    private Path resourceDirectory = Paths.get( "resources", "netPositionsData");
    private String absolutePath = resourceDirectory.toFile().getAbsolutePath();
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
    private MergingTask task;

    @Autowired
    private CeMergingConfiguration configuration;
    @Autowired
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    private MergingTaskRepository repository;
    private NetPositionService netPositionService;
    private String loadflowParametersFile;

    @BeforeEach
    void setUp() {
        igmFr = new IgmData();
        igmFr.setCountry("FR");
        igmFr.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_FR0.uct"));

        igmAt = new IgmData();
        igmAt.setCountry("AT");
        igmAt.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_AT1.uct"));

        igmBe = new IgmData();
        igmBe.setCountry("BE");
        igmBe.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_BE0.UCT"));

        igmCz = new IgmData();
        igmCz.setCountry("CZ");
        igmCz.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_CZ0.UCT"));

        igmHr = new IgmData();
        igmHr.setCountry("HR");
        igmHr.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_HR1.uct"));

        igmHu = new IgmData();
        igmHu.setCountry("HU");
        igmHu.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_HU0.uct"));

        igmNl = new IgmData();
        igmNl.setCountry("NL");
        igmNl.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_NL0.uct"));

        igmPl = new IgmData();
        igmPl.setCountry("PL");
        igmPl.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_PL0.uct"));

        igmRo = new IgmData();
        igmRo.setCountry("RO");
        igmRo.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_RO0.UCT"));

        igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmSi.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_SI0.UCT"));

        igmSk = new IgmData();
        igmSk.setCountry("SK");
        igmSk.setIgmFilePath(absolutePath.concat("/20190618_0030_2D2_SK0.UCT"));

        igmEs = new IgmData();
        igmEs.setCountry("ES");
        igmEs.setIgmFilePath(absolutePath.concat("/20190617_0030_FO1_ES0.UCT"));

        igmDeFile = new SavedFile("germanPreMergedIgmFile.uct", absolutePath.concat("/germanPreMergedIgmFile.uct"), "mock");
        igmDkFile = new SavedFile("denmarkRenamedIgmFile.uct", absolutePath.concat("/denmarkRenamedIgmFile.uct"), "mock");

        loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        repository = mock(MergingTaskRepository.class);
        when(repository.save(anyTask())).thenAnswer(invocation -> invocation.getArgument(0));
        netPositionService = new NetPositionService(repository, configuration, loadFlowRunnerSupplier);
    }

    @Test
    public void computeInitialNetPositionsTest() throws IOException {

        List<IgmData> igms = Arrays.asList(igmFr, igmBe, igmAt, igmCz, igmHr, igmHu, igmNl, igmPl, igmRo, igmSi, igmSk, igmEs);

        Artifacts artifacts = new Artifacts();
        artifacts.putFile(DK_CONVERTED_FILE, igmDkFile);
        artifacts.putFile(GERMAN_PRE_MERGED_IGM, igmDeFile);

        Inputs inputs = new Inputs();
        inputs.setTargetDate(BEGINNING_OF_2000);
        inputs.setIgms(igms);

        Configurations configurations = new Configurations();

        task = new MergingTask();
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
        assertEquals(1519.39, netPositions.get(AT).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(1519.39, netPositions.get(AT).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(647.06, netPositions.get(AT).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(647.06, netPositions.get(AT).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(13224.91, netPositions.get(FR).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(11224.91, netPositions.get(FR).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(5258.87, netPositions.get(FR).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(5258.87, netPositions.get(FR).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-347.01, netPositions.get(BE).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-347.01, netPositions.get(BE).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-347.01, netPositions.get(BE).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-347.01, netPositions.get(BE).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(984.07, netPositions.get(CZ).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(984.07, netPositions.get(CZ).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(997.37, netPositions.get(CZ).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(997.37, netPositions.get(CZ).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-738.59, netPositions.get(HR).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-738.59, netPositions.get(HR).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-306.20, netPositions.get(HR).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-306.20, netPositions.get(HR).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-879.59, netPositions.get(HU).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-879.59, netPositions.get(HU).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-163.99, netPositions.get(HU).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-163.99, netPositions.get(HU).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-2128.81, netPositions.get(NL).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-2560.80, netPositions.get(NL).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-2560.80, netPositions.get(NL).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-2560.80, netPositions.get(NL).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-79.99, netPositions.get(PL).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(17.00, netPositions.get(PL).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(17.00, netPositions.get(PL).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(17.00, netPositions.get(PL).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(1040.17, netPositions.get(RO).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(1040.17, netPositions.get(RO).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(217.46, netPositions.get(RO).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(217.46, netPositions.get(RO).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(488.99, netPositions.get(SI).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(488.99, netPositions.get(SI).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(66.99, netPositions.get(SI).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(66.99, netPositions.get(SI).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(200.00, netPositions.get(SK).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(200.00, netPositions.get(SK).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-215.00, netPositions.get(SK).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-215.00, netPositions.get(SK).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-4043.53, netPositions.get(DE).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-3455.59, netPositions.get(DE).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(-2887.96, netPositions.get(DE).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(-2887.96, netPositions.get(DE).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);

        assertEquals(-1354.30, netPositions.get(DK).getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(590.4, netPositions.get(DK).getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        assertEquals(0., netPositions.get(DK).getInRegionNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(0., netPositions.get(DK).getInRegionNetPosition().getWithoutVirtualHubs(), 0.1);
    }

    private void testDetailedExchanges(NetPositionsResults netPositionsFile) {
        NetPositions netPositionsFR = netPositionsFile.get(FR);
        Map<String, Double> globalDetailedExchanges = netPositionsFR.getGlobalDetailedExchanges();
        assertEquals(6, globalDetailedExchanges.size());
        assertEquals(13224.91, globalDetailedExchanges.values().stream().mapToDouble(v -> v).sum(), 0.1);

        assertEquals(13224.91 - 11224.91, netPositionsFR.getVirtualHubsExchanges().values().stream().mapToDouble(v -> v).sum(), 0.1);

        NetPositions netPositionsSK = netPositionsFile.get(SK);
        globalDetailedExchanges = netPositionsSK.getGlobalDetailedExchanges();
        assertEquals(4, globalDetailedExchanges.size());
        assertEquals(200.00, globalDetailedExchanges.values().stream().mapToDouble(v -> v).sum(), 0.1);

    }

    private void testOutBciNetPositions(NetPositionsResults netPositionsFile) {
        NetPositions netPositionsEs = netPositionsFile.get(ES);
        double sumVirtualHubs = netPositionsEs.getVirtualHubsExchanges().values().stream().mapToDouble(v -> v).sum();
        assertEquals(sumVirtualHubs, netPositionsEs.getGlobalNetPosition().getWithVirtualHubs() - netPositionsEs.getGlobalNetPosition().getWithoutVirtualHubs(), 0.1);
        double sumExchanges = netPositionsEs.getGlobalDetailedExchanges().values().stream().mapToDouble(v -> v).sum();
        assertEquals(sumExchanges, netPositionsEs.getGlobalNetPosition().getWithVirtualHubs(), 0.1);
        assertEquals(9.3, netPositionsEs.getGlobalDetailedExchanges().get("MA"), 0.1);
        assertEquals(9.3, netPositionsEs.getOutBciNetPosition(), 0.1);

    }

    private void testGenerationAndLoad(NetPositionsResults netPositionsFile) {
        assertEquals(7003.58, netPositionsFile.get(AT).getGenerationAndLoadQuantity().generation(), 0.1);
        assertEquals(5414.91, netPositionsFile.get(AT).getGenerationAndLoadQuantity().load(), 0.1);
    }
}

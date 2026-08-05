/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.german;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import test_utils.TaskTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.stringPathOf;

@SpringBootTest
@ActiveProfiles("OpenLoadFlow")
public class GermanMismatchCompensationTest {
    @Autowired
    private GermanMismatchCompensation germanMismatchCompensation;

    @Autowired
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    private MergingTask task;

    @Autowired
    private CeMergingConfiguration configuration;
    private IgmData igmD2;
    private IgmData igmD4;
    private IgmData igmD6;
    private IgmData igmD7;
    private IgmData igmD8;
    private List<VirtualHubRecord> virtualHubList;
    private List<XnodeConfig> xnodeList;

    @BeforeEach
    void setUp() throws IOException {

        igmD2 = new IgmData();
        igmD2.setCountry("D2");
        igmD2.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_D21.uct"));

        igmD4 = new IgmData();
        igmD4.setCountry("D4");
        igmD4.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_D41.uct"));

        igmD6 = new IgmData();
        igmD6.setCountry("D6");
        igmD6.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_D60.uct"));

        igmD7 = new IgmData();
        igmD7.setCountry("D7");
        igmD7.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_D71.UCT"));

        igmD8 = new IgmData();
        igmD8.setCountry("D8");
        igmD8.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_D80.UCT"));

        List<IgmData> igms = Arrays.asList(igmD2, igmD4, igmD6, igmD7, igmD8);
        Artifacts artifacts = new Artifacts();

        Inputs inputs = new Inputs();
        inputs.setIgms(igms);

        task = new MergingTask();
        task.setInputs(inputs);
        task.setArtifacts(artifacts);

        TaskTestUtils.setTaskDefaultConfigurations(task);

        String loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        //TaskTestUtils.setLoadflowParameters(task, loadflowParametersFile);

        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        cacheXNodesInformation(task);
    }

    @Test
    public void applyMismatchTest() throws FileNotFoundException {
        Network mergedNetwork = merge(task);
        germanMismatchCompensation.apply(task, mergedNetwork);
        checkInitialGermanNetPositions(task);

        NetPositionsResults netPositionsFile = new NetPositionsResults(new HashMap<>());
        LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        loadFlowRunnerSupplier.get().run(mergedNetwork, loadFlowParameters);
        //NetPositionCalculationHandler netPositionCalculationHandler = new NetPositionCalculationHandler();
        //netPositionCalculationHandler.computeNetPositions(task.getConfigurations().getRegionConfiguration(), mergedNetwork, Country.DE, netPositionsFile, virtualHubList, xnodeList, LoadFlowParameters.ComponentMode.MAIN_CONNECTED);
        assertEquals(1, netPositionsFile.netPositionsByCountryMap().size());
        assertEquals(-587.94, netPositionsFile.get("DE").getVirtualHubsExchanges().get("XBW_BJ1D"), 0.01); //check virtual hubs exchanges didn't change
        double sumInitialNetPositionWithoutVirtualHubs = -1926.55;
        double sumInitialNetPositionWithVirtualHubs = -2514.49;
        double sumInitialLoad = 25849.4;
        double sumInitialGeneration = 23849.4;
        //A difference under 1% from initial loads is acceptable by Coreso
        assertTrue((netPositionsFile.get("DE").getGenerationAndLoadQuantity().generation() - sumInitialGeneration) / sumInitialGeneration < 0.01);
        assertTrue((netPositionsFile.get("DE").getGenerationAndLoadQuantity().load() - sumInitialLoad) / sumInitialLoad < 0.01);
        assertTrue(Math.abs((netPositionsFile.get("DE").getGlobalNetPosition().getWithoutVirtualHubs() - sumInitialNetPositionWithoutVirtualHubs) / sumInitialNetPositionWithoutVirtualHubs) < 0.04);
        assertTrue(Math.abs((netPositionsFile.get("DE").getGlobalNetPosition().getWithVirtualHubs() - sumInitialNetPositionWithVirtualHubs) / sumInitialNetPositionWithVirtualHubs) < 0.04);
    }

    private void checkInitialGermanNetPositions(MergingTask task) throws FileNotFoundException {
        NetPositionsResults germanNetPositionResults = task.getArtifact(ArtifactType.GERMAN_IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);
        double sumIntNetPositions = germanMismatchCompensation.getInitialInternalNetPosition(germanNetPositionResults);
        assertEquals(1464.6, sumIntNetPositions, 1);
        double sumExtNetPositions = getInitialExternalNetPosition(germanNetPositionResults);
        assertEquals(-3391.1, sumExtNetPositions, 1);
        double sumInitialNetPositionWithoutVirtualHubs = sumExtNetPositions + sumIntNetPositions;
        assertEquals(-354.9 - 1991.1 - 417.8 - 5126.7 + 5376.0 - (-587.9), sumInitialNetPositionWithoutVirtualHubs, 1); //values from convergence
        double sumInitialNetPositionWithoutVirtualHubs2 = germanNetPositionResults.netPositionsByCountryMap().values().stream().mapToDouble(netPositions -> netPositions.getGlobalNetPosition().getWithoutVirtualHubs()).sum();
        assertEquals(sumInitialNetPositionWithoutVirtualHubs2, sumInitialNetPositionWithoutVirtualHubs, 1);
        double generationD2 = germanNetPositionResults.get("D2").getGenerationAndLoadQuantity().generation();
        double generationD4 = germanNetPositionResults.get("D4").getGenerationAndLoadQuantity().generation();
        double generationD6 = germanNetPositionResults.get("D6").getGenerationAndLoadQuantity().generation();
        double generationD7 = germanNetPositionResults.get("D7").getGenerationAndLoadQuantity().generation();
        double generationD8 = germanNetPositionResults.get("D8").getGenerationAndLoadQuantity().generation();
        double sumInitialGeneration = generationD2 + generationD4 + generationD6 + generationD7 + generationD8;
        assertEquals(23849.7, sumInitialGeneration, 1);
        double loadD2 = germanNetPositionResults.get("D2").getGenerationAndLoadQuantity().load();
        double loadD4 = germanNetPositionResults.get("D4").getGenerationAndLoadQuantity().load();
        double loadD6 = germanNetPositionResults.get("D6").getGenerationAndLoadQuantity().load();
        double loadD7 = germanNetPositionResults.get("D7").getGenerationAndLoadQuantity().load();
        double loadD8 = germanNetPositionResults.get("D8").getGenerationAndLoadQuantity().load();
        double sumInitialLoad = loadD2 + loadD4 + loadD6 + loadD7 + loadD8;
        assertEquals(25849.5, sumInitialLoad, 1);
    }

    private Network merge(MergingTask task) {
        Properties parameters = new Properties();
        parameters.put("ucte.import.create-areas", "false");
        List<Network> networksToMerge = Arrays.stream(GermanTso.values())
                .map(tso -> {
                    Path path = Paths.get(task.getInputs().getIgm(tso.name()).getIgmFile().getPath()); // NOSONAR File location does not come from user input
                    return Network.read(path, LocalComputationManager.getDefault(), ImportConfig.CACHE.get(), parameters);
                })
                .toList();
        return Network.merge("merged-network", networksToMerge.toArray(Network[]::new));
    }

    private void cacheXNodesInformation(MergingTask task) {
        xnodeList = task.getConfigurations().getXnodeList();
        virtualHubList = task.getConfigurations().getVirtualHubList();
    }

    private Double getInitialExternalNetPosition(NetPositionsResults germanNetPositionResults) {
        double sumInitialExternalNP = 0;
        for (GermanTso germanTso : GermanTso.values()) {
            final String tso = germanTso.name();
            double tsoInternalNP = germanNetPositionResults.get(tso)
                    .getGlobalDetailedExchanges().getOrDefault("DE", 0.);
            // we consider that all virtual hubs exchanges are external exchanges
            double tsoExternalNP = germanNetPositionResults.get(tso).getGlobalNetPosition().getWithoutVirtualHubs() - tsoInternalNP;
            sumInitialExternalNP += tsoExternalNP;
        }
        return sumInitialExternalNP;
    }

}

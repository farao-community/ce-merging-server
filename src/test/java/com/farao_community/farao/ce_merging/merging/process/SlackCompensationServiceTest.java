/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("OpenLoadFlow")
class SlackCompensationServiceTest {

    @Autowired
    SlackCompensationService slackCompensationService;

    @Autowired
    private CeMergingConfiguration configuration;

    private MergingTask task1 = new MergingTask();
    private MergingTask task2 = new MergingTask();
    private MergingTask task3 = new MergingTask();

    private static String slackNodeFound;

    @TestConfiguration
    @Profile("OpenLoadFlow")
    static class PlatformConfigTestContextConfig {

        @Bean
        @Primary
        public Supplier<LoadFlow.Runner> testLoadflowSupplier() {
            return this::getLoadFlowRunner;
        }

        private LoadFlow.Runner getLoadFlowRunner() {
            return new LoadFlow.Runner(new OpenLoadFlowProvider());
        }

    }

    @BeforeEach
    void setUp() throws IOException {
        Inputs inputs = new Inputs();
        inputs.setTargetDate(OffsetDateTime.parse("2019-06-17T22:30Z"));

        IgmData igmEs = new IgmData();
        igmEs.setCountry("ES");
        igmEs.setIgmFilePath("20190617_0030_FO1_ES0.UCT");

        List<IgmData> igms = Collections.singletonList(igmEs);
        inputs.setIgms(igms);

        SavedFile cgmFileWithoutSlackNode = new SavedFile("20190618_0030_2D2_UC0_withoutSlackNode.uct", "20190618_0030_2D2_UC0_withoutSlackNode.uct", "mock");
        Artifacts artifacts = new Artifacts();
        artifacts.putFile(CGM_FILE_AFTER_PST, cgmFileWithoutSlackNode);

        task1.setId(1L);
        task1.setInputs(inputs);
        task1.setArtifacts(artifacts);
        TaskTestUtils.setTaskDefaultConfigurations(task1);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task1)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task1)));

        IgmData igmEsWithoutSlackNode = new IgmData();
        igmEsWithoutSlackNode.setCountry("ES");
        igmEsWithoutSlackNode.setIgmFilePath("20190617_0030_FO1_ES1.UCT");

        List<IgmData> igms2 = Collections.singletonList(igmEsWithoutSlackNode);
        Inputs inputs2 = new Inputs();
        inputs2.setTargetDate(OffsetDateTime.parse("2019-11-17T08:30Z"));
        inputs2.setIgms(igms2);
        task2.setId(2L);
        task2.setInputs(inputs2);
        task2.setArtifacts(artifacts);
        TaskTestUtils.setTaskDefaultConfigurations(task2);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task2)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task2)));

        task3.setId(3L);
        task3.setInputs(inputs2);
        Artifacts artifacts3 = new Artifacts();
        SavedFile cgmFileWithoutNode = new SavedFile("20190618_0030_2D2_UC0_withoutNode.uct", "20190618_0030_2D2_UC0_withoutNode.uct", "mock");
        artifacts3.putFile(CGM_FILE_AFTER_PST, cgmFileWithoutNode);
        task3.setArtifacts(artifacts3);

        TaskTestUtils.setTaskDefaultConfigurations(task3);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task3)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task3)));
    }

    @Test
    void shouldAddEsSlackNodeTest() {
        slackNodeFound = "ELA MU11";
        Network networkCgm = createNetworkWithBus("ELA MU11");
        slackCompensationService.addSlackNode(networkCgm, task1);
        List<SlackTerminal> slackTerminals = getSlackTerminals(networkCgm);
        assertEquals(1, slackTerminals.size());
        assertEquals("VL_ELA MU11", slackTerminals.getFirst().getTerminal().getVoltageLevel().getId());
    }

    @Test
    void shouldAddDefaultSlackNode() {
        slackNodeFound = null;
        task2.getConfigurations().setDefaultSlackNode("ELA MU1");
        Network networkCgm = createNetworkWithBus("ELA MU1");
        assertTrue(getSlackTerminals(networkCgm).isEmpty());

        slackCompensationService.addSlackNode(networkCgm, task2);
        List<SlackTerminal> slackTerminals = getSlackTerminals(networkCgm);
        assertEquals(1, slackTerminals.size());
        assertEquals("VL_ELA MU1", slackTerminals.getFirst().getTerminal().getVoltageLevel().getId());
    }

    private List<SlackTerminal> getSlackTerminals(Network network) {
        return network.getVoltageLevelStream()
            .map(vl -> vl.getExtension(SlackTerminal.class))
            .filter(Objects::nonNull)
            .map(SlackTerminal.class::cast)
            .toList();
    }


    @Test
    void shouldNotAddSlackNodeIfNotExistingNode() {
        slackNodeFound = null;
        task3.getConfigurations().setDefaultSlackNode("NON_EXISTING");
        Network networkCgm = createNetworkWithBus("ELA MU1");
        assertTrue(getSlackTerminals(networkCgm).isEmpty());

        slackCompensationService.addSlackNode(networkCgm, task3);
        assertTrue(getSlackTerminals(networkCgm).isEmpty());
    }



    private static Network createNetworkWithBus(String busId) {
        Network network = Network.create("network", "source");
        VoltageLevel vl = network.newVoltageLevel().setId("VL_" + busId).setNominalV(225).setTopologyKind(TopologyKind.BUS_BREAKER).add();
        vl.getBusBreakerView().newBus().setId(busId).add();
        vl.newLoad().setId("L_" + busId).setBus(busId).setP0(0).setQ0(0).add();
        return network;
    }


}
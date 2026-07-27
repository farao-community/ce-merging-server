/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.slack_compensation;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.SlackTerminal;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XIIDM_FORMAT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.iidm.network.TopologyKind.BUS_BREAKER;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("OpenLoadFlow")
class SlackCompensationServiceTest {

    @Autowired
    SlackCompensationService slackCompensationService;

    @MockBean
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    @Autowired
    private CeMergingConfiguration configuration;

    private final MergingTask task1 = new MergingTask();
    private final MergingTask task2 = new MergingTask();
    private final MergingTask task3 = new MergingTask();

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
        final Inputs inputs = new Inputs();
        inputs.setTargetDate(OffsetDateTime.parse("2019-06-17T22:30Z"));
        final IgmData igmEs = new IgmData();
        igmEs.setCountry("ES");
        igmEs.setIgmFilePath("20190617_0030_FO1_ES0.UCT");
        inputs.setIgms(singletonList(igmEs));

        final SavedFile cgmFileWithoutSlackNode = new SavedFile("20190618_0030_2D2_UC0_withoutSlackNode.uct", "20190618_0030_2D2_UC0_withoutSlackNode.uct", "mock");
        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(CGM_FILE_AFTER_PST, cgmFileWithoutSlackNode);

        task1.setId(1L);
        task1.setInputs(inputs);
        task1.setArtifacts(artifacts);

        final IgmData igmEsWithoutSlackNode = new IgmData();
        igmEsWithoutSlackNode.setCountry("ES");
        igmEsWithoutSlackNode.setIgmFilePath("20190617_0030_FO1_ES1.UCT");
        final Inputs inputs2 = new Inputs();
        inputs2.setTargetDate(OffsetDateTime.parse("2019-11-17T08:30Z"));
        inputs2.setIgms(singletonList(igmEsWithoutSlackNode));
        task2.setId(2L);
        task2.setInputs(inputs2);
        task2.setArtifacts(artifacts);

        TaskTestUtils.setTaskDefaultConfigurations(task2);

        task3.setId(3L);
        task3.setInputs(inputs2);
        final Artifacts artifacts3 = new Artifacts();
        final SavedFile cgmFileWithoutNode = new SavedFile("20190618_0030_2D2_UC0_withoutNode.uct", "20190618_0030_2D2_UC0_withoutNode.uct", "mock");
        artifacts3.putFile(CGM_FILE_AFTER_PST, cgmFileWithoutNode);
        task3.setArtifacts(artifacts3);

        for (final MergingTask task : List.of(task1, task2, task3)) {
            TaskTestUtils.setTaskDefaultConfigurations(task);
            Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
            Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        }
    }

    @Test
    void shouldAddEsSlackNodeTest() {
        final Network cgm = createNetworkWithBus("ELA MU11");
        slackCompensationService.addSlackNode(cgm, task1);
        final List<SlackTerminal> slackTerminals = getSlackTerminals(cgm);
        assertEquals(1, slackTerminals.size());
        assertEquals("VL_ELA MU11", slackTerminals.getFirst().getTerminal().getVoltageLevel().getId());
    }

    @Test
    void shouldAddDefaultSlackNode() {
        task2.getConfigurations().setDefaultSlackNode("ELA MU1");
        final Network cgm = createNetworkWithBus("ELA MU1");
        assertTrue(getSlackTerminals(cgm).isEmpty());

        slackCompensationService.addSlackNode(cgm, task2);
        final List<SlackTerminal> slackTerminals = getSlackTerminals(cgm);
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
        task3.getConfigurations().setDefaultSlackNode("NON_EXISTING");
        final Network cgm = createNetworkWithBus("ELA MU1");
        assertTrue(getSlackTerminals(cgm).isEmpty());

        slackCompensationService.addSlackNode(cgm, task3);
        assertTrue(getSlackTerminals(cgm).isEmpty());
    }

    private static Network createNetworkWithBus(String busId) {
        final Network network = Network.create("network", "source");

        final VoltageLevel vl = network.newVoltageLevel()
            .setId("VL_" + busId)
            .setNominalV(225)
            .setTopologyKind(BUS_BREAKER)
            .add();
        vl.getBusBreakerView().newBus().setId(busId).add();
        vl.newLoad().setId("L_" + busId).setBus(busId).setP0(0).setQ0(0).add();

        return network;
    }

    @Test
    void shouldCompensateNetworkTest() {
        final LoadFlow.Runner mockRunner = mock(LoadFlow.Runner.class);
        when(loadFlowRunnerSupplier.get()).thenReturn(mockRunner);

        final String loadId = "L1";
        final String generatorId = "G1";

        // mock OLF
        when(mockRunner.run(any(Network.class), any(LoadFlowParameters.class))).thenAnswer(call -> {
            final Network cgm = call.getArgument(0);
            cgm.getLoad(loadId).getTerminal().setP(110);
            cgm.getLoad(loadId).getTerminal().setQ(60);
            cgm.getGenerator(generatorId).getTerminal().setP(-110);
            cgm.getGenerator(generatorId).getTerminal().setQ(-60);

            LoadFlowResult.ComponentResult componentResult = mock(LoadFlowResult.ComponentResult.class);
            when(componentResult.getStatus()).thenReturn(CONVERGED);
            when(componentResult.getSynchronousComponentNum()).thenReturn(MAIN_NUM);
            LoadFlowResult result = mock(LoadFlowResult.class);
            when(result.getComponentResults()).thenReturn(singletonList(componentResult));
            return result;
        });

        task1.getConfigurations().setLoadFlowParameters(new LoadFlowParameters());
        final String busId = "BUS1";
        final Network cgmBeforeCompensation = Network.create("cgmBeforeCompensation", "source");
        final VoltageLevel vl = cgmBeforeCompensation.newVoltageLevel()
            .setId("VL1")
            .setNominalV(225)
            .setTopologyKind(BUS_BREAKER)
            .add();

        vl.getBusBreakerView().newBus().setId(busId).add();
        vl.newLoad()
            .setId(loadId)
            .setBus(busId)
            .setP0(100)
            .setQ0(50)
            .add();

        vl.newGenerator()
            .setId(generatorId)
            .setBus(busId)
            .setTargetP(100)
            .setTargetQ(50)
            .setMinP(-9999)
            .setMaxP(9999)
            .setVoltageRegulatorOn(false)
            .add();

        final Path tempFile = Paths.get(configuration.getArtifactsDirectoryPath(task1), "test_compensate.xiidm");
        cgmBeforeCompensation.write(XIIDM_FORMAT, null, tempFile);

        task1.getArtifacts().putFile(CGM_FILE_AFTER_PST, new SavedFile("test_compensate.xiidm",
                                                                       tempFile.toString(),
                                                                       "mock"));

        final Network cgmAfterCompensation = slackCompensationService.compensateNetwork(task1);

        final Load loadAfterCompensation = cgmAfterCompensation.getLoad(loadId);
        final Generator generatorAfterCompensation = cgmAfterCompensation.getGenerator(generatorId);

        assertEquals(110.0, loadAfterCompensation.getP0(), 0.1);
        assertEquals(60.0, loadAfterCompensation.getQ0(), 0.1);
        assertEquals(110.0, generatorAfterCompensation.getTargetP(), 0.1);
        assertEquals(60.0, generatorAfterCompensation.getTargetQ(), 0.1);
    }

}

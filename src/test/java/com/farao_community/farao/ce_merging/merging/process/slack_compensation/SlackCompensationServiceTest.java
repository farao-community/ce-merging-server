/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.slack_compensation;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
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
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XIIDM_FORMAT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.iidm.network.Country.ES;
import static com.powsybl.iidm.network.TopologyKind.BUS_BREAKER;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.createMockTask;
import static test_utils.CeTestUtils.createTempFolders;
import static test_utils.CeTestUtils.singletonArtifact;
import static test_utils.CeTestUtils.singletonIgmInputs;

@SpringBootTest
@ActiveProfiles("OpenLoadFlow")
class SlackCompensationServiceTest {

    @Autowired
    SlackCompensationService slackCompensationService;

    @MockitoBean
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    @Autowired
    private CeMergingConfiguration configuration;

    @TempDir
    Path tempDir;

    private MergingTask task1;
    private MergingTask task2;
    private MergingTask task3;

    @TestConfiguration
    @Profile("OpenLoadFlow")
    static class PlatformTestConfig {

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
    void setUp() {
        final Inputs inputsWoSlackNode = singletonIgmInputs(ES, "20190617_0030_FO1_ES1.UCT");
        final Artifacts artifactsWoSlackNode = singletonArtifact(CGM_FILE_AFTER_PST, "20190618_0030_2D2_UC0_withoutSlackNode.uct");

        task1 = createMockTask(1L, singletonIgmInputs(ES, "20190617_0030_FO1_ES0.UCT"), artifactsWoSlackNode);
        task2 = createMockTask(2L, inputsWoSlackNode, artifactsWoSlackNode);
        task3 = createMockTask(3L, inputsWoSlackNode, singletonArtifact(CGM_FILE_AFTER_PST, "20190618_0030_2D2_UC0_withoutNode.uct"));

        List.of(task1, task2, task3).forEach(task -> createTempFolders(task, tempDir, configuration));

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
    void shouldCompensateNetwork() {
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

        final Path tempFile = tempDir.resolve(Paths.get(configuration.getArtifactsDirectoryPath(task1), "test_compensate.xiidm"));
        cgmBeforeCompensation.write(XIIDM_FORMAT, null, tempFile);

        task1.getArtifacts().putFile(CGM_FILE_AFTER_PST, new SavedFile("test_compensate.xiidm",
                                                                       tempFile.toString(),
                                                                       "mock"));

        final Network cgmAfterCompensation = slackCompensationService.compensateFinalCgmSlackImbalance(task1);

        final Load loadAfterCompensation = cgmAfterCompensation.getLoad(loadId);
        final Generator generatorAfterCompensation = cgmAfterCompensation.getGenerator(generatorId);

        assertEquals(110.0, loadAfterCompensation.getP0());
        assertEquals(60.0, loadAfterCompensation.getQ0());
        assertEquals(110.0, generatorAfterCompensation.getTargetP());
        assertEquals(60.0, generatorAfterCompensation.getTargetQ());
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.process.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodeIncomplete;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodeIncorrect;
import com.farao_community.farao.ce_merging.merging.process.recessivity.inconsistencies.XnodesInconsistencies;
import com.farao_community.farao.ce_merging.merging.process.xnode.AreaInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodeInformation;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCheck;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import test_utils.TaskTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.CLOSE;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RecessivityServiceTest {

    @Autowired
    private RecessivityService recessivityService;

    @MockBean
    private MergingTaskRepository tasksRepository;

    private MergingTask task;
    private MergingTask taskFr;
    private MergingTask taskWithAlegro;

    @Autowired
    private CeMergingConfiguration configuration;

    private Network network;
    private Terminal terminal2;

    @BeforeEach
    void setUp() throws IOException {
        // grid
        network = mock(Network.class);
        Branch branch = mock(Branch.class);
        Terminal terminal1 = mock(Terminal.class);
        terminal2 = mock(Terminal.class);
        VoltageLevel vl1 = mock(VoltageLevel.class);
        VoltageLevel vl2 = mock(VoltageLevel.class);
        Substation ss1 = mock(Substation.class);
        Substation ss2 = mock(Substation.class);

        when(branch.getId()).thenReturn("XAC_LO11");
        when(branch.getTerminal(ONE)).thenReturn(terminal1);
        when(branch.getTerminal(TWO)).thenReturn(terminal2);
        when(terminal1.getVoltageLevel()).thenReturn(vl1);
        when(terminal2.getVoltageLevel()).thenReturn(vl2);
        when(vl1.getSubstation()).thenReturn(Optional.of(ss1));
        when(vl2.getSubstation()).thenReturn(Optional.of(ss2));
        when(ss1.getCountry()).thenReturn(Optional.of(BE));
        when(ss2.getCountry()).thenReturn(Optional.of(FR));
        when(ss2.getNullableCountry()).thenReturn(FR);

        when(network.getBranchStream()).thenAnswer(invocation -> Stream.of(branch));

        // task data

        IgmData igmFr = new IgmData();
        igmFr.setCountry("FR");
        IgmData igmD6 = new IgmData();
        igmD6.setCountry("D6");
        IgmData igmBe = new IgmData();
        igmBe.setCountry("BE");
        IgmData igmNl = new IgmData();
        igmNl.setCountry("NL");
        List<IgmData> igms = Arrays.asList(igmFr, igmBe, igmNl, igmD6);

        Map<String, XnodeInformation> xnodeInformationMap = new HashMap<>();
        xnodeInformationMap.put("XAC_LO11", new XnodeInformation(new AreaInformation("BE", CLOSE), new AreaInformation("FR", OPEN)));
        // Add some more to match previous test expectations if needed, but the logic is what matters
        for (int i = 0; i < 40; i++) {
            xnodeInformationMap.put("XINCOMPLETE_" + i, new XnodeInformation(new AreaInformation("BE", OPEN), null));
        }
        // Specific ones from old test
        xnodeInformationMap.put("XBE_GB1B", new XnodeInformation(new AreaInformation("BE", OPEN), null));
        xnodeInformationMap.put("XBE_OX21", new XnodeInformation(new AreaInformation("BE", OPEN), null));

        XnodesCheck xnodesCheck = new XnodesCheck(xnodeInformationMap);

        task = mock(MergingTask.class);
        taskFr = mock(MergingTask.class);
        taskWithAlegro = mock(MergingTask.class);

        Inputs inputs = new Inputs();
        inputs.setIgms(igms);
        OffsetDateTime odt = OffsetDateTime.now(ZoneId.of("Europe/Paris"));
        inputs.setTargetDate(odt);

        Configurations configurations = mock(Configurations.class);
        List<String> recessivityCountries = Arrays.asList("FR", "BE", "NL", "DE", "ES", "PT", "IT", "CH", "AT", "SI", "HR", "PL", "CZ", "HU");
        when(configurations.getOrDefaultRecessiveCountries()).thenReturn(recessivityCountries);
        TaskTestUtils.setTaskDefaultConfigurations(task); // This sets XnodeList

        when(task.getInputs()).thenReturn(inputs);
        when(task.getConfigurations()).thenReturn(configurations);
        when(task.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class)).thenReturn(xnodesCheck);
        when(task.getArtifact(TOPOLOGICAL_MERGE_FILE, Network.class)).thenReturn(network);
        when(task.getArtifact(XNODES_INCONSISTENCIES, XnodesInconsistencies.class)).thenCallRealMethod();
        Artifacts mockArtifacts = mock(Artifacts.class);
        when(task.getArtifacts()).thenReturn(mockArtifacts);
        when(task.getId()).thenReturn(1L);

        when(taskFr.getInputs()).thenReturn(inputs);
        Configurations configurationsFr = mock(Configurations.class);
        when(configurationsFr.getOrDefaultRecessiveCountries()).thenReturn(List.of("FR"));
        when(taskFr.getConfigurations()).thenReturn(configurationsFr);
        when(taskFr.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class)).thenReturn(xnodesCheck);
        when(taskFr.getArtifact(TOPOLOGICAL_MERGE_FILE, Network.class)).thenReturn(network);
        when(taskFr.getArtifact(XNODES_INCONSISTENCIES, XnodesInconsistencies.class)).thenCallRealMethod();
        when(taskFr.getArtifacts()).thenReturn(mockArtifacts);
        when(taskFr.getId()).thenReturn(2L);

        when(taskWithAlegro.getConfigurations()).thenReturn(configurations);
        Map<String, XnodeInformation> xnodeInformationMapAlegro = new HashMap<>();
        xnodeInformationMapAlegro.put("XLI_OB1A", new XnodeInformation(new AreaInformation("D7", OPEN), null));
        xnodeInformationMapAlegro.put("XLI_OB1B", new XnodeInformation(new AreaInformation("BE", CLOSE), null));
        XnodesCheck xnodesCheckAlegro = new XnodesCheck(xnodeInformationMapAlegro);
        when(taskWithAlegro.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class)).thenReturn(xnodesCheckAlegro);
        when(taskWithAlegro.getArtifact(XNODES_INCONSISTENCIES, XnodesInconsistencies.class)).thenCallRealMethod();

        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(taskFr)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskFr)));
    }

    @Test
    public void getRecessiveCountriesFromConfiguration() {
        List<String> recessivityCountries = task.getConfigurations().getOrDefaultRecessiveCountries();
        assertEquals(14, recessivityCountries.size());
    }

    @Test
    public void applyRecessivity() {
        try (MockedStatic<FileStorageUtils> fileStorage = mockStatic(FileStorageUtils.class)) {
            recessivityService.applyRecessivity(task);

            fileStorage.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INCONSISTENCIES),
                                                                       argThat(o -> hasIncorrectXnode(o, "XAC_LO11")
                                                                                    && hasNumberOfIncompleteXnodes(o, 42)),
                                                                       eq(task),
                                                                       eq(configuration)));

        }
    }

    @Test
    public void applyRecessivityFR() {
        try (MockedStatic<FileStorageUtils> fileStorage = mockStatic(FileStorageUtils.class)) {
            recessivityService.applyRecessivity(taskFr);

            fileStorage.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INCONSISTENCIES),
                                                                       argThat(o -> hasIncorrectXnode(o, "XAC_LO11")),
                                                                       eq(taskFr),
                                                                       eq(configuration)));

            verify(terminal2).connect();
        }
    }

    private boolean hasIncorrectXnode(final Object artifact, final String nodeId) {
        if (artifact instanceof final XnodesInconsistencies inc) {
            List<XnodeIncorrect> xnodesIncorrect = inc.getXnodeIncorrectList();
            return xnodesIncorrect.size() == 1 && xnodesIncorrect.getFirst().getName().equals(nodeId);
        }
        return false;
    }

    private boolean hasNumberOfIncompleteXnodes(final Object artifact, final int count) {
        if (artifact instanceof final XnodesInconsistencies inc) {
            List<XnodeIncomplete> xnodesIncorrect = inc.getXnodeIncompleteList();
            return xnodesIncorrect.size() == count;
        }
        return false;
    }

    @Test
    public void checkAlegroXnodesInconsistenciesTest() throws FileNotFoundException {
        List<XnodeIncorrect> xnodeIncorrectsList = new ArrayList<>();
        List<String> recessivityCountries = taskWithAlegro.getConfigurations().getOrDefaultRecessiveCountries();
        XnodesCheck xnodesCheck = taskWithAlegro.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class);
        recessivityService.checkAlegroXnodes(xnodeIncorrectsList, xnodesCheck.getXnodeInformationMap(), recessivityCountries);

        assertEquals(1, xnodeIncorrectsList.size());
        assertEquals("XLI_OB1", xnodeIncorrectsList.getFirst().getName());
        assertEquals("BE", xnodeIncorrectsList.getFirst().getCountry1());
        assertEquals("D7", xnodeIncorrectsList.getFirst().getCountry2());
        assertEquals(CLOSE, xnodeIncorrectsList.getFirst().getStatus1());
        assertEquals(OPEN, xnodeIncorrectsList.getFirst().getStatus2());
        assertEquals(OPEN, xnodeIncorrectsList.getFirst().getFinalStatus());
    }

}

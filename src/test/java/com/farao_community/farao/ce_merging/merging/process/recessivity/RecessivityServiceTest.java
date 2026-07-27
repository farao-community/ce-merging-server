/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.recessivity;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.process.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies.XnodeIncomplete;
import com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies.XnodeIncorrect;
import com.farao_community.farao.ce_merging.merging.process.xnode.inconsistencies.XnodesInconsistencies;
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
import test_utils.TaskTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ALEGRO_NODE_PREFIX;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_NODE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_NODE_NAME;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.CLOSE;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TOPOLOGICAL_MERGE_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INCONSISTENCIES;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.GermanTso.D6;
import static com.farao_community.farao.ce_merging.merging.task.enums.GermanTso.D7;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.FR;
import static com.powsybl.iidm.network.Country.NL;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static java.nio.file.Files.createDirectories;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class RecessivityServiceTest {

    private static final String BE_FR_NODE = "XAC_LO11";
    private static final String DE_DE_NODE = "XDE_LO11";

    @Autowired
    private RecessivityService recessivityService;

    @Autowired
    private MergingTaskRepository tasksRepository;

    private MergingTask task;
    private MergingTask taskFr;
    private MergingTask taskDe;
    private MergingTask taskWithAlegro;

    @Autowired
    private CeMergingConfiguration configuration;

    private Network network;
    private Terminal terminalBe;
    private Terminal terminal1De;
    private Terminal terminalFr;
    private Terminal terminal2De;

    @BeforeEach
    void setUp() throws IOException {
        prepareMockNetwork();

        task = mockTaskWithIdAndRecessiveCountries(1L, null);
        TaskTestUtils.setTaskDefaultConfigurations(task); // This sets XnodeList
        taskFr = mockTaskWithIdAndRecessiveCountries(2L, List.of(FR.name()));
        taskDe = mockTaskWithIdAndRecessiveCountries(3L, List.of(D6.name()));
        taskWithAlegro = mockAlegroTask();

        for (final MergingTask mock : List.of(task, taskDe, taskFr, taskWithAlegro)) {
            when(mock.getInputs()).thenReturn(mockInputs());
            when(mock.getArtifact(XNODES_INCONSISTENCIES, XnodesInconsistencies.class)).thenCallRealMethod();
        }

    }

    @Test
    void getRecessiveCountriesFromConfiguration() {
        final List<String> recessivityCountries = task.getConfigurations().getOrDefaultRecessiveCountries();
        assertEquals(14, recessivityCountries.size());
    }

    @Test
    void applyRecessivity() {
        try (final MockedStatic<FileStorageUtils> fileStorage = mockStatic(FileStorageUtils.class)) {
            recessivityService.applyRecessivity(task);

            fileStorage.verify(() -> FileStorageUtils.saveArtifactNetwork(eq(TGM_FILE_AFTER_RECESSIVITY), any(), any(), anyString(), isNull(), any()));
            fileStorage.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INCONSISTENCIES),
                                                                       argThat(o -> hasIncompleteXnode(o, "XBE_OX21")),
                                                                       any(), any()));

        }
    }

    @Test
    void applyRecessivityOnFrance() {
        try (final MockedStatic<FileStorageUtils> fileStorage = mockStatic(FileStorageUtils.class)) {
            recessivityService.applyRecessivity(taskFr);

            fileStorage.verify(() -> FileStorageUtils.saveArtifactNetwork(eq(TGM_FILE_AFTER_RECESSIVITY), any(), any(), anyString(), isNull(), any()));
            fileStorage.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INCONSISTENCIES),
                                                                       argThat(o -> hasIncorrectXnode(o, BE_FR_NODE)),
                                                                       any(), any()));

            verify(terminalFr).connect();
            verify(terminalFr, never()).disconnect();
            verify(terminalBe, never()).connect();
            verify(terminalBe, never()).disconnect();
        }
    }

    @Test
    void applyRecessivityOnGermany() {
        try (final MockedStatic<FileStorageUtils> fileStorage = mockStatic(FileStorageUtils.class)) {
            recessivityService.applyRecessivity(taskDe);

            fileStorage.verify(() -> FileStorageUtils.saveArtifactNetwork(eq(TGM_FILE_AFTER_RECESSIVITY), any(), any(), anyString(), isNull(), any()));
            fileStorage.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INCONSISTENCIES),
                                                                       argThat(o -> hasIncorrectXnode(o, DE_DE_NODE)),
                                                                       any(), any()));

            verify(terminal1De).disconnect();
            verify(terminal2De).disconnect();
            verify(terminal1De, never()).connect();
            verify(terminal2De, never()).connect();
        }
    }

    @Test
    void checkAlegroXnodesInconsistenciesTest() throws FileNotFoundException {
        final List<XnodeIncorrect> xnodeIncorrectsList = new ArrayList<>();
        final List<String> recessivityCountries = taskWithAlegro.getConfigurations().getOrDefaultRecessiveCountries();
        final XnodesCheck xnodesCheck = taskWithAlegro.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class);
        recessivityService.checkAlegroXnodes(xnodeIncorrectsList, xnodesCheck.getXnodeInformationMap(), recessivityCountries);

        assertEquals(1, xnodeIncorrectsList.size());
        final XnodeIncorrect xnodeIncorrect = xnodeIncorrectsList.getFirst();

        assertEquals(ALEGRO_NODE_PREFIX, xnodeIncorrect.getName());
        assertEquals(BE.name(), xnodeIncorrect.getCountry1());
        assertEquals(D7.name(), xnodeIncorrect.getCountry2());
        assertEquals(CLOSE, xnodeIncorrect.getStatus1());
        assertEquals(OPEN, xnodeIncorrect.getStatus2());
        assertEquals(OPEN, xnodeIncorrect.getFinalStatus());
    }

    /*
            MOCKS
     */

    private Inputs mockInputs() {
        final IgmData igmFr = new IgmData();
        igmFr.setCountry(FR.name());
        final IgmData igmD6 = new IgmData();
        igmD6.setCountry(D6.name());
        final IgmData igmD7 = new IgmData();
        igmD7.setCountry(D7.name());
        final IgmData igmBe = new IgmData();
        igmBe.setCountry(BE.name());
        final IgmData igmNl = new IgmData();
        igmNl.setCountry(NL.name());

        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(igmFr, igmBe, igmNl, igmD6, igmD7));
        inputs.setTargetDate(OffsetDateTime.now(PARIS_ZONE_ID));

        return inputs;
    }

    private void prepareMockNetwork() {
        network = mock(Network.class);
        final Branch<?> branch = mock(Branch.class);
        final Branch<?> branchDe = mock(Branch.class);
        terminalBe = mock(Terminal.class);
        terminalFr = mock(Terminal.class);
        terminal1De = mock(Terminal.class);
        terminal2De = mock(Terminal.class);
        final VoltageLevel vl1 = mock(VoltageLevel.class);
        final VoltageLevel vl2 = mock(VoltageLevel.class);
        final VoltageLevel vl1De = mock(VoltageLevel.class);
        final VoltageLevel vl2De = mock(VoltageLevel.class);
        final Substation ss1 = mock(Substation.class);
        final Substation ss2 = mock(Substation.class);
        final Substation ss1De = mock(Substation.class);
        final Substation ss2De = mock(Substation.class);

        when(branch.getId()).thenReturn(BE_FR_NODE);
        when(branch.getTerminal(ONE)).thenReturn(terminalBe);
        when(branch.getTerminal(TWO)).thenReturn(terminalFr);
        when(terminalBe.getVoltageLevel()).thenReturn(vl1);
        when(terminalFr.getVoltageLevel()).thenReturn(vl2);
        when(vl1.getSubstation()).thenReturn(Optional.of(ss1));
        when(vl2.getSubstation()).thenReturn(Optional.of(ss2));
        when(ss1.getCountry()).thenReturn(Optional.of(BE));
        when(ss2.getCountry()).thenReturn(Optional.of(FR));
        when(ss2.getNullableCountry()).thenReturn(FR);

        final String vlDeId = "D6D7" + DE_DE_NODE;
        when(branchDe.getId()).thenReturn(DE_DE_NODE);
        when(branchDe.getTerminal(ONE)).thenReturn(terminal1De);
        when(branchDe.getTerminal(TWO)).thenReturn(terminal2De);
        when(branchDe.getTerminal1()).thenReturn(terminal1De);
        when(branchDe.getTerminal2()).thenReturn(terminal2De);
        when(terminal1De.getVoltageLevel()).thenReturn(vl1De);
        when(terminal2De.getVoltageLevel()).thenReturn(vl2De);
        when(vl1De.getId()).thenReturn(vlDeId);
        when(vl2De.getId()).thenReturn(vlDeId);
        when(vl1De.getSubstation()).thenReturn(Optional.of(ss1De));
        when(vl2De.getSubstation()).thenReturn(Optional.of(ss2De));
        when(ss1De.getCountry()).thenReturn(Optional.of(DE));
        when(ss2De.getCountry()).thenReturn(Optional.of(DE));
        when(ss2De.getNullableCountry()).thenReturn(DE);

        when(network.getBranchStream()).thenAnswer(invocation -> Stream.of(branch, branchDe));
    }

    private MergingTask mockTaskWithIdAndRecessiveCountries(final long id, final List<String> recessive) throws IOException {
        final Artifacts artifacts = mock(Artifacts.class);
        final XnodesCheck xnodesCheck = new XnodesCheck(Map.of(
            BE_FR_NODE, new XnodeInformation(new AreaInformation(BE.name(), CLOSE),
                                             new AreaInformation(FR.name(), OPEN)),
            DE_DE_NODE, new XnodeInformation(new AreaInformation(D6.name(), CLOSE),
                                             new AreaInformation(D7.name(), OPEN)),
            "XBE_GB1B", new XnodeInformation(new AreaInformation(BE.name(), OPEN), null),
            "XBE_OX21", new XnodeInformation(new AreaInformation(BE.name(), OPEN), null)
        ));
        final MergingTask mock = mock(MergingTask.class);
        final Configurations mockCfg = mock(Configurations.class);
        if (recessive == null) {
            when(mockCfg.getOrDefaultRecessiveCountries()).thenCallRealMethod();
        } else {
            when(mockCfg.getOrDefaultRecessiveCountries()).thenReturn(recessive);
        }
        when(mock.getId()).thenReturn(id);
        when(mock.getArtifacts()).thenReturn(artifacts);
        when(mock.getArtifact(TOPOLOGICAL_MERGE_FILE, Network.class)).thenReturn(network);
        when(mock.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class)).thenReturn(xnodesCheck);
        when(mock.getConfigurations()).thenReturn(mockCfg);

        createDirectories(Paths.get(configuration.getOutputsDirectoryPath(mock)));
        createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(mock)));

        return mock;
    }

    private MergingTask mockAlegroTask() throws FileNotFoundException {
        final MergingTask mock = mock(MergingTask.class);
        final Configurations alegroCfg = mock(Configurations.class);
        when(alegroCfg.getOrDefaultRecessiveCountries()).thenCallRealMethod();
        when(mock.getConfigurations()).thenReturn(alegroCfg);
        when(mock.getArtifact(XNODES_INFORMATION_FILE, XnodesCheck.class)).thenReturn(getXnodesCheckAlegro());

        return mock;
    }

    private static XnodesCheck getXnodesCheckAlegro() {
        final Map<String, XnodeInformation> xnodeInformationMapAlegro = Map.of(
            VIRTUAL_HUB_ALEGRO_DE_NODE_NAME, new XnodeInformation(new AreaInformation(D7.name(), OPEN),
                                                                  null),
            VIRTUAL_HUB_ALEGRO_BE_NODE_NAME, new XnodeInformation(new AreaInformation(BE.name(), CLOSE),
                                                                  null)
        );
        return new XnodesCheck(xnodeInformationMapAlegro);
    }

    /*
            CHECKS
     */

    private boolean hasIncorrectXnode(final Object artifact, final String nodeId) {
        if (artifact instanceof final XnodesInconsistencies inc) {
            final List<XnodeIncorrect> xnodesIncorrect = inc.getXnodeIncorrectList();
            return xnodesIncorrect.stream().anyMatch(xn -> xn.getName().equals(nodeId));
        }
        return false;
    }

    private boolean hasIncompleteXnode(final Object artifact, final String nodeId) {
        if (artifact instanceof final XnodesInconsistencies inc) {
            final List<XnodeIncomplete> xnodesIncomplete = inc.getXnodeIncompleteList();
            return xnodesIncomplete.stream().anyMatch(xn -> xn.getName().equals(nodeId));
        }
        return false;
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.hvdc_alignment;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.*;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class HvdcXNodeAlignmentServiceTest {
    private static final String RESOURCES_PATH = "src/test/resources/hvdc_alignment";
    private static final String NL_COUNTRY_CODE = "NL";
    private static final String IT_COUNTRY_CODE = "IT";
    private static final String GR_COUNTRY_CODE = "GR";
    private static final String NL_IGM = "nl_igm.uct";
    private static final String IT_IGM = "it_igm.uct";
    private static final String GR_IGM = "gr_igm.uct";
    private static final String DE_IGM = "de_igm.uct";
    private static final String DK_IGM = "dk_igm.uct";
    private static final String XED_EE1N = "XED_EE1N";
    private static final String XED_EE1D = "XED_EE1D";
    private static final String XAR_GA1I = "XAR_GA1I";
    private static final String XAR_GA1G = "XAR_GA1G";

    @Autowired
    private HvdcXNodeAlignmentService hvdcXNodeAlignmentService;

    @Autowired
    private CeMergingConfiguration configuration;

    @MockitoBean
    private MergingTaskRepository tasksRepository;

    private MergingTask task = new MergingTask();

    @BeforeEach
    void setUp() throws IOException {
        final SavedFile igmNLFile = createSavedFile(NL_IGM);
        final SavedFile igmITFile = createSavedFile(IT_IGM);
        final SavedFile igmGRFile = createSavedFile(GR_IGM);
        final SavedFile igmDEFile = createSavedFile(DE_IGM);
        final SavedFile igmDKFile = createSavedFile(DK_IGM);
        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(
                createIgmData(NL_COUNTRY_CODE, igmNLFile),
                createIgmData(IT_COUNTRY_CODE, igmITFile),
                createIgmData(GR_COUNTRY_CODE, igmGRFile)
        ));

        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(ArtifactType.GERMAN_PRE_MERGED_IGM, igmDEFile);
        artifacts.putFile(ArtifactType.DK_CONVERTED_FILE,igmDKFile);
        task.setInputs(inputs);
        task.setArtifacts(artifacts);
        TaskTestUtils.setTaskDefaultConfigurations(task);
        Files.createDirectories(Paths.get(configuration.getInputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
    }

    @Test
    void setZeroFlowNodes() {
        hvdcXNodeAlignmentService.setZeroFlowNodes(task);
        Network network = readNetwork(task.getArtifacts().getFile(ArtifactType.GERMAN_PRE_MERGED_IGM).getPath());
        assertZeroFlow(network, "XBA_KF31");
        assertZeroFlow(network, "XBA_KF32");
    }

    @Test
    void applyHvdcXNodeAlignment() {
        final Network dkNetworkInitial = readNetwork(task.getArtifacts().getFile(ArtifactType.DK_CONVERTED_FILE).getPath());
        assertDanglingLine(dkNetworkInitial, XED_EE1D, 0.0, 0.0, -1.9, 248.6);
        final Network nlNetworkInitial = readInputNetwork(NL_COUNTRY_CODE);
        assertDanglingLine(nlNetworkInitial, XED_EE1N, 0.0, 0.0, 0.0, 0.0);
        final Network itNetworkInitial = readInputNetwork(IT_COUNTRY_CODE);
        assertDanglingLine(itNetworkInitial, XAR_GA1I, -485.1, 135.8, 0.0, 0.0);
        final Network grNetworkInitial = readInputNetwork(GR_COUNTRY_CODE);
        assertDanglingLine(grNetworkInitial, XAR_GA1G, 500.0, 28.0801, 0.0, 0.0);
        hvdcXNodeAlignmentService.applyHvdcXNodeAlignment(task);

        final Network dkNetworkAfterHvdcAlignment = readNetwork(task.getArtifacts().getFile(ArtifactType.DK_CONVERTED_FILE).getPath());
        assertDanglingLine(dkNetworkAfterHvdcAlignment, XED_EE1D, 0.0, 0.0, 0.0, 0.0);
        final Network nlNetworkAfterHvdcAlignment = readPreTreatedNetwork(NL_COUNTRY_CODE);
        assertDanglingLine(nlNetworkAfterHvdcAlignment, XED_EE1N, 0.0, 0.0, 0.0, 0.0);
        final Network itNetworkAfterHvdcAlignment = readPreTreatedNetwork(IT_COUNTRY_CODE);
        assertDanglingLine(itNetworkAfterHvdcAlignment, XAR_GA1I, -485.1, 135.8, 0.0, 0.0);
        final Network grNetworkAfterHvdcAlignment = readPreTreatedNetwork(GR_COUNTRY_CODE);
        assertDanglingLine(grNetworkAfterHvdcAlignment, XAR_GA1G, 485.1, 28.0801, 0.0, 0.0);
    }

    private static DanglingLine getDanglingLineFromNetwork(final Network network, final String nodeName) {
        return network.getDanglingLineStream()
                .filter(danglingLine -> danglingLine.getPairingKey().equals(nodeName))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find dangling line " + nodeName + " in network " + network.getNameOrId()));
    }

    private static Network readNetwork(final String path) {
        return Network.read(path);
    }

    private static SavedFile createSavedFile(final String fileName) {
        return new SavedFile(fileName, RESOURCES_PATH + "/" + fileName, "mock");
    }

    private static IgmData createIgmData(final String country, final SavedFile file) {
        IgmData igmData = new IgmData();
        igmData.setCountry(country);
        igmData.setIgmFile(file);
        return igmData;
    }

    private static void assertDanglingLine(final Network network,
                                           final String nodeName,
                                           final double expectedP0,
                                           final double expectedQ0,
                                           final double expectedTargetP,
                                           final double expectedTargetQ) {

        final DanglingLine danglingLine = getDanglingLineFromNetwork(network, nodeName);
        assertEquals(expectedP0, danglingLine.getP0(), 1e-6);
        assertEquals(expectedQ0, danglingLine.getQ0(), 1e-6);
        assertEquals(expectedTargetP, danglingLine.getGeneration().getTargetP(), 1e-6);
        assertEquals(expectedTargetQ, danglingLine.getGeneration().getTargetQ(), 1e-6);
    }

    private static void assertZeroFlow(final Network network, final String nodeName) {
        final DanglingLine danglingLine = getDanglingLineFromNetwork(network, nodeName);

        assertEquals(0.0, danglingLine.getP0());
        assertEquals(0.0, Math.abs(danglingLine.getGeneration().getTargetP()));
    }

    private Network readInputNetwork(final String country) {
        return readNetwork(task.getInputs().getIgm(country).getIgmFile().getPath());
    }

    private Network readPreTreatedNetwork(final String country) {
        return readNetwork(task.getArtifacts().getPreTreatedIgmMap().get(country).getPath());
    }
}

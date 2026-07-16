/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.PstOutput;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("testForPST")
class PstSpecialServiceOpenLoadFlowTest {

    @Autowired
    PstSpecialService pstSpecialService;
    @Autowired
    private CeMergingConfiguration configuration;

    private final Path resourceDirectory = Paths.get("src", "test", "resources", "pst_special");
    private final String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    private final MergingTask entityForProcess2 = new MergingTask();
    private final MergingTask entityForProcess2PadriocianoOutage = new MergingTask();
    private final MergingTask entityForProcess2DivacaOutage = new MergingTask();
    private IgmData igmSi;
    private IgmData igmIt;
    private IgmData igmAt;
    private SavedFile shiftedCgm;

    @TestConfiguration()
    @Profile("testForPST")
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
        String loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        Configurations configurations = new Configurations();

        entityForProcess2.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess2, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess2))); // NOSONAR File location does not come from user input
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess2))); // NOSONAR File location does not come from user input

        entityForProcess2PadriocianoOutage.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess2PadriocianoOutage, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess2PadriocianoOutage))); // NOSONAR File location does not come from user input
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess2PadriocianoOutage))); // NOSONAR File location does not come from user input

        entityForProcess2DivacaOutage.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess2DivacaOutage, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess2DivacaOutage))); // NOSONAR File location does not come from user input
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess2DivacaOutage))); // NOSONAR File location does not come from user input

        Inputs inputs = new Inputs();
        igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmIt = new IgmData();
        igmIt.setCountry("IT");
        igmAt = new IgmData();
        igmAt.setCountry("AT");
        inputs.setTargetDate(OffsetDateTime.parse("2020-02-19T00:30Z"));
    }

    @Test
    void testPstProcess2() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process2/20200218_0030_FO2_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process2/20200219_0030_2D3_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process2/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        Inputs inputs = new Inputs();
        inputs.setIgms(igms);
        inputs.setTargetDate(OffsetDateTime.parse("2020-02-19T00:30Z"));

        Artifacts artifacts = new Artifacts();
        shiftedCgm = new SavedFile("/20200219_0030_2D3_UX0.uct", absolutePath.concat("/process2/20200219_0030_2D3_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);

        entityForProcess2.setInputs(inputs);
        entityForProcess2.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess2);
        SavedFile pstOutputFile = entityForProcess2.getArtifacts().getFile(PST_OUTPUT_FILE);

        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(800., result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(150., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(650., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(8, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(150., result.getFlowDivacaPadriciano().getFlowCGM(), 10.0);
        assertEquals(650., result.getFlowDivacaRedipuglia().getFlowCGM(), 10.0);
    }

    @Test
    void testPstProcess2PadricianoOutage() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process2/padricianoOutage/20200710_2230_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process2/padricianoOutage/20200713_2230_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process2/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        Inputs inputs = new Inputs();
        inputs.setIgms(igms);
        inputs.setTargetDate(OffsetDateTime.parse("2020-07-13T22:30Z"));

        Artifacts artifacts = new Artifacts();
        shiftedCgm = new SavedFile("/20200713_2230_2D1_UX0.uct", absolutePath.concat("/process2/padricianoOutage/20200713_2230_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);

        entityForProcess2PadriocianoOutage.setInputs(inputs);
        entityForProcess2PadriocianoOutage.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess2PadriocianoOutage);
        SavedFile pstOutputFile = entityForProcess2PadriocianoOutage.getArtifacts().getFile(PST_OUTPUT_FILE);

        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(1.0, result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(1., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0., result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(-16., result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(0, result.getFlowDivacaPadriciano().getFlowCGM(), 1.0);
        assertEquals(-7., result.getFlowDivacaRedipuglia().getFlowCGM(), 1.0);

    }

    @Test
    void testPstProcess2DivacaOutage() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process2/divacaOutage/20200710_2130_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process2/divacaOutage/20200713_2130_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process2/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        Inputs inputs = new Inputs();
        inputs.setIgms(igms);
        inputs.setTargetDate(OffsetDateTime.parse("2020-07-13T21:30Z"));

        Artifacts artifacts = new Artifacts();
        shiftedCgm = new SavedFile("/20200713_2130_2D1_UX0.uct", absolutePath.concat("/process2/divacaOutage/20200713_2130_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);

        entityForProcess2DivacaOutage.setInputs(inputs);
        entityForProcess2DivacaOutage.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess2DivacaOutage);
        SavedFile pstOutputFile = entityForProcess2DivacaOutage.getArtifacts().getFile(PST_OUTPUT_FILE);

        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(1.0, result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(150., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(150., result.getFlowDivacaPadriciano().getFlowCGM(), 10.0);
        assertEquals(0., result.getFlowDivacaRedipuglia().getFlowCGM(), 1.0);

    }
}

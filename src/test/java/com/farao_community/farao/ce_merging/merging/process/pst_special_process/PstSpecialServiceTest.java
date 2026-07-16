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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.PADRICIANO;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PstSpecialServiceTest {

    @Autowired
    PstSpecialService pstSpecialService;
    @Autowired
    private CeMergingConfiguration configuration;

    private final Path resourceDirectory = Paths.get("src", "test", "resources", "pst_special");
    private final String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    private final MergingTask entityForProcess1 = new MergingTask();
    private final MergingTask entityForProcess1PadriocianoOutage = new MergingTask();
    private final MergingTask entityForProcess1DivacaOutage = new MergingTask();
    private final MergingTask entityForProcess3 = new MergingTask();
    private final MergingTask entityError = new MergingTask();
    private IgmData igmSi;
    private IgmData igmIt;
    private IgmData igmAt;
    private SavedFile shiftedCgm;
    private Inputs inputs;
    private Artifacts artifacts;

    @BeforeEach
    void setUp() throws IOException {
        String loadflowParametersFile = "pst_special/ac-load-flow-parameters.json";
        Configurations configurations = new Configurations();

        entityError.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityError, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityError)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityError)));

        entityForProcess1.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess1, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess1)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess1)));

        entityForProcess1PadriocianoOutage.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess1PadriocianoOutage, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess1PadriocianoOutage)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess1PadriocianoOutage)));

        entityForProcess1DivacaOutage.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess1DivacaOutage, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess1DivacaOutage)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess1DivacaOutage)));

        entityForProcess3.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(entityForProcess3, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(entityForProcess3)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(entityForProcess3)));

        inputs = new Inputs();
        artifacts = new Artifacts();
        igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmIt = new IgmData();
        igmIt.setCountry("IT");
        igmAt = new IgmData();
        igmIt.setCountry("AT");
        inputs.setTargetDate(OffsetDateTime.parse("2020-02-19T00:30Z"));
    }

    @Test
    void testPstProcess1() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process1/20200218_0030_FO2_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        entityForProcess1.setInputs(inputs);

        shiftedCgm = new SavedFile("/20200219_0030_2D3_UX0.uct", absolutePath.concat("/process1/20200219_0030_2D3_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        entityForProcess1.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess1);

        SavedFile pstOutputFile = entityForProcess1.getArtifacts().getFile(PST_OUTPUT_FILE);
        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0., result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(-6., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }

    @Test
    void testPstProcess1DivacaOutage() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process1/divacaOutage/20200710_2130_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/divacaOutage/20200713_2130_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        entityForProcess1DivacaOutage.setInputs(inputs);

        shiftedCgm = new SavedFile("/20200713_2130_2D1_UX0.uct", absolutePath.concat("/process1/divacaOutage/20200713_2130_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        entityForProcess1DivacaOutage.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess1DivacaOutage);
        SavedFile pstOutputFile = entityForProcess1DivacaOutage.getArtifacts().getFile(PST_OUTPUT_FILE);
        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }

    @Test
    void testPstProcess1PadricianoOutage() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process1/padricianoOutage/20200710_2230_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/padricianoOutage/20200713_2230_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        entityForProcess1PadriocianoOutage.setInputs(inputs);

        shiftedCgm = new SavedFile("/20200713_2230_2D1_UX0.uct", absolutePath.concat("/process1/padricianoOutage/20200713_2230_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        entityForProcess1PadriocianoOutage.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess1PadriocianoOutage);
        SavedFile pstOutputFile = entityForProcess1PadriocianoOutage.getArtifacts().getFile(PST_OUTPUT_FILE);
        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }

    @Test
    void testPstProcess3() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process3/20200710_2330_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process3/20200713_2330_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        Inputs inputs = new Inputs();
        inputs.setIgms(igms);
        inputs.setTargetDate(OffsetDateTime.parse("2020-07-13T23:30Z"));

        Artifacts artifacts = new Artifacts();
        shiftedCgm = new SavedFile("/20200713_2330_2D1_UX0.uct", absolutePath.concat("/process3/20200713_2330_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);

        entityForProcess3.setInputs(inputs);
        entityForProcess3.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityForProcess3);
        SavedFile pstOutputFile = entityForProcess3.getArtifacts().getFile(PST_OUTPUT_FILE);

        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(3, result.getProcessNumberDivaca());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }

    @Test
    void testPstDivacaWithNoTargetFlow() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/error/20200218_0030_FO2_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/error/20200219_0030_2D3_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20200219_0030_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        entityError.setInputs(inputs);

        shiftedCgm = new SavedFile("/20200219_0030_2D3_UX0.uct", absolutePath.concat("/error/20200219_0030_2D3_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        entityError.setArtifacts(artifacts);

        pstSpecialService.fixPst(entityError);
        SavedFile pstOutputFile = entityError.getArtifacts().getFile(PST_OUTPUT_FILE);
        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaPadriciano(), 0.0);
        assertEquals(0., result.getTargetFlowDivacaRedipuglia(), 0.0);
        assertEquals(NaN, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(NaN, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(-6., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }
}

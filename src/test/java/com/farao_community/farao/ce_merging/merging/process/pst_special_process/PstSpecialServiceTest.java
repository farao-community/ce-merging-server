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

    private final MergingTask taskP1 = new MergingTask();
    private final MergingTask taskP1PadricianoOut = new MergingTask();
    private final MergingTask taskP1DivacaOut = new MergingTask();
    private final MergingTask taskP3 = new MergingTask();
    private final MergingTask taskFailed = new MergingTask();
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

        taskFailed.setId(1L);
        taskFailed.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(taskFailed, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskFailed)));

        taskP1.setId(2L);
        taskP1.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(taskP1, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskP1)));

        taskP1PadricianoOut.setId(3L);
        taskP1PadricianoOut.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(taskP1PadricianoOut, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskP1PadricianoOut)));

        taskP1DivacaOut.setId(4L);
        taskP1DivacaOut.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(taskP1DivacaOut, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskP1DivacaOut)));

        taskP3.setId(5L);
        taskP3.setConfigurations(configurations);
        TaskTestUtils.setLoadflowParameters(taskP3, loadflowParametersFile);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(taskP3)));

        inputs = new Inputs();
        artifacts = new Artifacts();
        igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmIt = new IgmData();
        igmIt.setCountry("IT");
        igmAt = new IgmData();
        igmAt.setCountry("AT");
        inputs.setTargetDate(OffsetDateTime.parse("2020-02-19T00:30Z"));
    }

    @Test
    void testPstProcess1() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_FO2_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        taskP1.setInputs(inputs);

        shiftedCgm = new SavedFile("/20260620_1130_2D3_UX0.uct", absolutePath.concat("/process1/20260620_1130_2D3_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        taskP1.setArtifacts(artifacts);

        pstSpecialService.fixPst(taskP1);

        SavedFile pstOutputFile = taskP1.getArtifacts().getFile(PST_OUTPUT_FILE);
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
        igmIt.setIgmFilePath(absolutePath.concat("/process1/divacaOutage/20260620_1130_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/divacaOutage/20260620_1130_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        taskP1DivacaOut.setInputs(inputs);

        shiftedCgm = new SavedFile("/20260620_1130_2D1_UX0.uct", absolutePath.concat("/process1/divacaOutage/20260620_1130_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        taskP1DivacaOut.setArtifacts(artifacts);

        pstSpecialService.fixPst(taskP1DivacaOut);
        SavedFile pstOutputFile = taskP1DivacaOut.getArtifacts().getFile(PST_OUTPUT_FILE);
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
        igmIt.setIgmFilePath(absolutePath.concat("/process1/padricianoOutage/20260620_1130_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process1/padricianoOutage/20260620_1130_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        taskP1PadricianoOut.setInputs(inputs);

        shiftedCgm = new SavedFile("/20260620_1130__2230_2D1_UX0.uct", absolutePath.concat("/process1/padricianoOutage/20260620_1130_2D1_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        taskP1PadricianoOut.setArtifacts(artifacts);

        pstSpecialService.fixPst(taskP1PadricianoOut);
        SavedFile pstOutputFile = taskP1PadricianoOut.getArtifacts().getFile(PST_OUTPUT_FILE);
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
        igmIt.setIgmFilePath(absolutePath.concat("/process3/20260620_1130_FO5_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/process3/20260620_1130_2D1_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        Inputs in = new Inputs();
        in.setIgms(igms);
        in.setTargetDate(OffsetDateTime.parse("2026-06-19T23:30Z"));

        Artifacts af = new Artifacts();
        shiftedCgm = new SavedFile("/20260620_1130_2D1_UX0.uct", absolutePath.concat("/process3/20260620_1130_2D1_UX0.uct"), "mock");
        af.putFile(BALANCED_CGM_FILE, shiftedCgm);

        taskP3.setInputs(in);
        taskP3.setArtifacts(af);

        pstSpecialService.fixPst(taskP3);
        SavedFile pstOutputFile = taskP3.getArtifacts().getFile(PST_OUTPUT_FILE);

        PstOutput result = JsonUtils.read(PstOutput.class, pstOutputFile.getPath());
        assertEquals(3, result.getProcessNumberDivaca());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM(), 0.0);
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM(), 0.0);
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM(), 0.0);
    }

    @Test
    void testPstDivacaWithNoTargetFlow() throws FileNotFoundException {
        igmIt.setIgmFilePath(absolutePath.concat("/error/20260620_1130_FO2_IT0.uct"));
        igmSi.setIgmFilePath(absolutePath.concat("/error/20260620_1130_2D3_SI0.UCT"));
        igmAt.setIgmFilePath(absolutePath.concat("/process1/20260620_1130_2D3_AT3.uct"));
        List<IgmData> igms = Arrays.asList(igmIt, igmSi, igmAt);
        inputs.setIgms(igms);
        taskFailed.setInputs(inputs);

        shiftedCgm = new SavedFile("/20260620_1130_2D3_UX0.uct", absolutePath.concat("/error/20260620_1130_2D3_UX0.uct"), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);
        taskFailed.setArtifacts(artifacts);

        pstSpecialService.fixPst(taskFailed);
        SavedFile pstOutputFile = taskFailed.getArtifacts().getFile(PST_OUTPUT_FILE);
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

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.PstOutput;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.PADRICIANO;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static java.lang.Double.NaN;
import static java.nio.file.Files.createDirectories;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.TaskTestUtils.setLoadflowParameters;

@SpringBootTest
@ActiveProfiles("testForPST")
class PstSpecialServiceTest {

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

    @Autowired
    PstSpecialService pstSpecialService;
    @Autowired
    private CeMergingConfiguration configuration;

    private final Path resourceDirectory = Paths.get("src", "test", "resources", "pst_special");
    private final String absolutePath = resourceDirectory.toFile().getAbsolutePath();

    @Test
    void shouldApplyProcess1WhenNoDivacaTargetFlow() throws IOException {
        final MergingTask task = createTask("/process1/20260620_1130_FO2_IT0.uct",
                                            "/process1/20260620_1130_2D3_SI0.UCT",
                                            "/process1/20260620_1130_2D3_AT3.uct",
                                            "/process1/20260620_1130_2D3_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca());
        assertEquals(0., result.getTargetFlowDivacaPadriciano());
        assertEquals(0., result.getTargetFlowDivacaRedipuglia());
        assertEquals(0., result.getTap(DIVACA).getTapIGM());
        assertEquals(0., result.getTap(DIVACA).getTapCGM());
        assertEquals(-6., result.getTap(PADRICIANO).getTapIGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM());
    }

    @Test
    void shouldHandleProcess1WithDivacaOutage() throws IOException {
        final MergingTask task = createTask("/process1/divacaOutage/20260620_1130_FO5_IT0.uct",
                                            "/process1/divacaOutage/20260620_1130_2D1_SI0.UCT",
                                            "/process1/20260620_1130_2D3_AT3.uct",
                                            "/process1/divacaOutage/20260620_1130_2D1_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca());
        assertEquals(0., result.getTargetFlowDivacaPadriciano());
        assertEquals(0., result.getTargetFlowDivacaRedipuglia());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM());
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM());
    }

    @Test
    void shouldHandleProcess1WithPadricianoOutage() throws IOException {
        final MergingTask task = createTask("/process1/padricianoOutage/20260620_1130_FO5_IT0.uct",
                                            "/process1/padricianoOutage/20260620_1130_2D1_SI0.UCT",
                                            "/process1/20260620_1130_2D3_AT3.uct",
                                            "/process1/padricianoOutage/20260620_1130_2D1_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca());
        assertEquals(0., result.getTargetFlowDivacaPadriciano());
        assertEquals(0., result.getTargetFlowDivacaRedipuglia());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM());
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM());
    }

    @Test
    void shouldHandleProcess1WithError() throws IOException {
        final MergingTask task = createTask("/error/20260620_1130_FO2_IT0.uct",
                                            "/error/20260620_1130_2D3_SI0.UCT",
                                            "/process1/20260620_1130_2D3_AT3.uct",
                                            "/error/20260620_1130_2D3_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(1, result.getProcessNumberDivaca());
        assertEquals(0., result.getTotalTargetFlowDivaca());
        assertEquals(0., result.getTargetFlowDivacaPadriciano());
        assertEquals(0., result.getTargetFlowDivacaRedipuglia());
        assertEquals(NaN, result.getTap(DIVACA).getTapIGM());
        assertEquals(NaN, result.getTap(DIVACA).getTapCGM());
        assertEquals(-6., result.getTap(PADRICIANO).getTapIGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM());
    }

    @Test
    void shouldApplyProcess2IfDivacaHasTargetFlow() throws IOException {
        final MergingTask task = createTask("/process2/20260620_1130_FO2_IT0.uct",
                                            "/process2/20260620_1130_2D3_SI0.UCT",
                                            "/process2/20260620_1130_2D3_AT3.uct",
                                            "/process2/20260620_1130_2D3_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(12.6, result.getTotalTargetFlowDivaca());
        assertEquals(150., result.getTargetFlowDivacaPadriciano());
        assertEquals(-137.4, result.getTargetFlowDivacaRedipuglia());
        assertEquals(0, result.getTap(DIVACA).getTapIGM());
        assertEquals(0, result.getTap(DIVACA).getTapCGM());
        assertEquals(-1128, result.getFlowDivacaPadriciano().getFlowCGM(), 10.0);
        assertEquals(0., result.getFlowDivacaRedipuglia().getFlowCGM(), 10.0);
    }

    @Test
    void shouldHandleProcess2WithPadricianoOutage() throws IOException {
        final MergingTask task = createTask("/process2/padricianoOutage/20260620_1130_FO5_IT0.uct",
                                            "/process2/padricianoOutage/20260620_1130_2D1_SI0.UCT",
                                            "/process2/20260620_1130_2D3_AT3.uct",
                                            "/process2/padricianoOutage/20260620_1130_2D1_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(3.389, result.getTotalTargetFlowDivaca());
        assertEquals(0., result.getTargetFlowDivacaPadriciano());
        assertEquals(3.389, result.getTargetFlowDivacaRedipuglia());
        assertEquals(0., result.getTap(DIVACA).getTapIGM());
        assertEquals(0., result.getTap(DIVACA).getTapCGM());
        assertEquals(9, result.getFlowDivacaPadriciano().getFlowCGM(), 1.0);
        assertEquals(0, result.getFlowDivacaRedipuglia().getFlowCGM(), 1.0);

    }

    @Test
    void shouldHandleProcess2WithDivacaOutage() throws IOException {
        final MergingTask task = createTask("/process2/divacaOutage/20260620_1130_FO5_IT0.uct",
                                            "/process2/divacaOutage/20260620_1130_2D1_SI0.UCT",
                                            "/process2/20260620_1130_2D3_AT3.uct",
                                            "/process2/divacaOutage/20260620_1130_2D1_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(2, result.getProcessNumberDivaca());
        assertEquals(2.865, result.getTotalTargetFlowDivaca());
        assertEquals(150., result.getTargetFlowDivacaPadriciano());
        assertEquals(0., result.getTargetFlowDivacaRedipuglia());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM());
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM());
        assertEquals(430, result.getFlowDivacaPadriciano().getFlowCGM(), 10.0);
        assertEquals(0., result.getFlowDivacaRedipuglia().getFlowCGM(), 1.0);

    }

    @Test
    void shouldApplyProcess3WhenDivacaPadricianoOut() throws IOException {
        final MergingTask task = createTask("/process3/20260620_1130_FO5_IT0.uct",
                                            "/process3/20260620_1130_2D1_SI0.UCT",
                                            "/process1/20260620_1130_2D3_AT3.uct",
                                            "/process3/20260620_1130_2D1_UX0.uct");

        pstSpecialService.fixPst(task);
        final PstOutput result = task.getArtifact(PST_OUTPUT_FILE, PstOutput.class);

        assertEquals(3, result.getProcessNumberDivaca());
        assertEquals(0.0, result.getTap(DIVACA).getTapIGM());
        assertEquals(0.0, result.getTap(DIVACA).getTapCGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapIGM());
        assertEquals(0., result.getTap(PADRICIANO).getTapCGM());
    }

    private MergingTask createTask(final String itIgmPath,
                                   final String siIgmPath,
                                   final String atIgmPath,
                                   final String shiftedCgmPath) throws IOException {

        final MergingTask task = new MergingTask();
        task.setId(1L);

        final Inputs inputs = createInputs(itIgmPath, siIgmPath, atIgmPath);

        final Artifacts artifacts = new Artifacts();
        final SavedFile shiftedCgm = new SavedFile(shiftedCgmPath.substring(shiftedCgmPath.lastIndexOf("/")),
                                                   absolutePath.concat(shiftedCgmPath), "mock");
        artifacts.putFile(BALANCED_CGM_FILE, shiftedCgm);

        task.setInputs(inputs);
        task.setArtifacts(artifacts);

        final String loadflowParametersFile = "pst_special/ac-load-flow-parameters.json";
        final Configurations configurations = new Configurations();

        task.setConfigurations(configurations);

        setLoadflowParameters(task, loadflowParametersFile);
        createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));

        return task;
    }

    private Inputs createInputs(final String itIgmPath,
                                final String siIgmPath,
                                final String atIgmPath) {
        final Inputs inputs = new Inputs();

        final IgmData igmSi = new IgmData();
        igmSi.setCountry("SI");
        igmSi.setIgmFilePath(absolutePath.concat(siIgmPath));
        final IgmData igmIt = new IgmData();
        igmIt.setCountry("IT");
        igmIt.setIgmFilePath(absolutePath.concat(itIgmPath));
        final IgmData igmAt = new IgmData();
        igmAt.setCountry("AT");
        igmAt.setIgmFilePath(absolutePath.concat(atIgmPath));

        inputs.setIgms(List.of(igmAt, igmIt, igmSi));
        inputs.setTargetDate(OffsetDateTime.parse("2026-06-19T23:30Z"));
        return inputs;
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_CORRECTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static java.lang.Double.isNaN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.stringPathOf;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class BalancesAdjustmentProcessorTest {

    private static final OffsetDateTime PROCESS_TARGET_DATE = OffsetDateTime.parse("2019-06-17T22:30Z");

    @Autowired
    private CeMergingConfiguration configuration;

    private MergingTask task;
    private final Supplier<LoadFlow.Runner> testLoadflowSupplier = this::getLoadFlowRunner;
    private final Supplier<BalanceComputationParameters> testBalanceComputationParameters = testBalanceComputationParameters();

    private LoadFlow.Runner getLoadFlowRunner() {
        return new LoadFlow.Runner(new OpenLoadFlowProvider());
    }

    private LoadFlowParameters getLoadFlowParameters() {
        LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.setDc(true);
        return parameters;
    }

    public Supplier<BalanceComputationParameters> testBalanceComputationParameters() {
        LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.setDc(true);
        return () -> new BalanceComputationParameters(1, 5).setLoadFlowParameters(getLoadFlowParameters());
    }

    @BeforeEach
    void setUp() throws IOException {
        //inputsFile
        final File glskFile = new File(getClass().getResource("/balances/20210723-F226-v1.xml").getFile());
        final File networkFile = new File(getClass().getResource("/balances/20210723_0030_2D1_UC5_F100_CORESO.uct").getFile());
        final File targetFile = new File(getClass().getResource("/balances/NetPositions.json").getFile());
        final File bapFile = new File(getClass().getResource("/balances/balances-adjustment-parameters.json").getFile());
        final RegionConfiguration regionConfiguration = new ObjectMapper().readValue(
            Files.readString(Paths.get(getClass().getResource("/balances/region_configuration.json").getPath())),
            RegionConfiguration.class
        );

        task = new MergingTask();
        task.setId(0L);
        task.getInputs().setTargetDate(PROCESS_TARGET_DATE);
        task.getArtifacts().putFile(GLSK_QUALITY_CORRECTED_FILE, new SavedFile("glsk",
                                                                               glskFile.getPath(),
                                                                               "glsk"));

        task.getArtifacts().putFile(TGM_FILE_AFTER_RECESSIVITY, new SavedFile("tgm",
                                                                              networkFile.getPath(),
                                                                              "tgm"));
        task.getArtifacts().putFile(BALANCES_ADJUSTMENT_TARGET_FILE, new SavedFile("target",
                                                                                   targetFile.getPath(),
                                                                                   "target"));

        task.getConfigurations().setRegionConfiguration(regionConfiguration);
        task.getConfigurations().setBalancesAdjustmentParameters(new SavedFile("bap",
                                                                               bapFile.getPath(),
                                                                               "bap"));

        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));

    }

    @Test
    void shouldRunProcess(final CapturedOutput capturedOutput) throws IOException {
        final BalancesAdjustmentProcessor processor = new BalancesAdjustmentProcessor(task, configuration, testLoadflowSupplier, testBalanceComputationParameters);
        assertDoesNotThrow(processor::run);
        final Network result = Network.read(stringPathOf("balances/20210723_0030_2D1_UC5_F100_CORESO.uct"));
        result.getGeneratorStream().forEach(generator -> {
            if (!isNaN(generator.getTerminal().getP())) {
                assertEquals(generator.getTargetP(), -generator.getTerminal().getP());
            }
        });

        assertThat(capturedOutput)
            .contains("Balance adjustment successful with AC loadflow after 2 iterations.");
    }

    @Test
    void shouldNotBalance(final CapturedOutput capturedOutput) throws IOException {
        final File targetFile = new File(getClass().getResource("/balances/NetPositions_failed.json").getFile());
        task.getArtifacts().putFile(BALANCES_ADJUSTMENT_TARGET_FILE, new SavedFile("target",
                                                                                   targetFile.getPath(),
                                                                                   "target"));

        final BalancesAdjustmentProcessor processor = new BalancesAdjustmentProcessor(task, configuration, testLoadflowSupplier, testBalanceComputationParameters);
        assertDoesNotThrow(processor::run);
        final Network result = Network.read(stringPathOf("balances/20210723_0030_2D1_UC5_F100_CORESO.uct"));
        assertTrue(result.getGeneratorStream()
                        .anyMatch(g -> g.getTargetP() + g.getTerminal().getP() != 0));
        assertThat(capturedOutput).contains("Important mismatch between initial total net positions");
    }

}

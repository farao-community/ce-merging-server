/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAlegroFlows;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciProcessResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.BciResultUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileSystemUtils;
import test_utils.CeTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.ALEGRO_NET_POSITIONS;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BCI_OUTPUT_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.assertions.BciAreaResultsAssert.assertThat;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

@SpringBootTest
class BciProcessTest {
    private BciProcess bciProcess;

    private String correctNpfPath;
    private String correctEcPath;
    private String correctFeasibilityRangePathAbsolute;
    private String correctFeasibilityRangePath;
    private String correctInitialNetPositionsPath;

    private String npfWithAlegroXnodesPath;
    private String ecWithAlegroXnodesPath;
    private String alegroDataPath;
    private String initialNetPositionsPath;

    private OffsetDateTime processTargetDate = OffsetDateTime.parse("2019-06-18T00:30Z");

    private RegionConfiguration regionConfiguration;
    private String jsonEicConfig;

    @Autowired
    private CeMergingConfiguration configuration;

    @BeforeEach
    void setUp() throws IOException {
        correctNpfPath = stringPathOf("bci/forecastReferenceProgram.json");
        correctEcPath = stringPathOf("bci/20190618_MergedEC.xml");
        correctFeasibilityRangePathAbsolute = stringPathOf("bci/20190618_bciFeasibilityRange_absolute.xml");
        correctFeasibilityRangePath = stringPathOf("bci/20190618_bciFeasibilityRange.xml");
        correctInitialNetPositionsPath = stringPathOf("bci/20190618_initialNetPositions.json");

        npfWithAlegroXnodesPath = stringPathOf("bci/forecastReferenceProgram.json");
        ecWithAlegroXnodesPath = stringPathOf("bci/20200608_MergedEC.xml");
        alegroDataPath = stringPathOf("bci/alegroNetPositions.json");
        initialNetPositionsPath = stringPathOf("bci/igmsNetPositions.json");

        ObjectMapper objectMapper = new ObjectMapper();
        File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        jsonEicConfig = new String(readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonEicConfig, RegionConfiguration.class);
    }

    @AfterEach
    void teardown() {
        FileSystemUtils.deleteRecursively(new File(configuration.getCeMergingRoot()));
    }

    @Test
    void runProcessWithoutFeasibilityRanges() throws IOException {
        final MergingTask task = prepareTask(0L,
                                             correctNpfPath,
                                             correctEcPath,
                                             null,
                                             null,
                                             null);

        bciProcess = new BciProcess(task, configuration);
        bciProcess.run();

        final BciProcessResult result = getResult(task);
        assertEquals("CE", result.regionName());
        assertEquals(processTargetDate, result.processDateTime());
        final Map<String, BciAreaResults> results = result.bciComputationResult().bciResults();
        assertEquals(12, results.size());
        assertThat(results.get("N11")).hasInTargetNp(702);
    }

    @Test
    void runProcessWithAbsoluteFeasibilityRanges() throws IOException {

        final MergingTask task = prepareTask(1L,
                                             correctNpfPath,
                                             correctEcPath,
                                             null,
                                             correctFeasibilityRangePathAbsolute,
                                             null);

        bciProcess = new BciProcess(task, configuration);
        bciProcess.run();

        final BciProcessResult result = getResult(task);
        assertEquals("CE", result.regionName());
        assertEquals(processTargetDate, result.processDateTime());
        final Map<String, BciAreaResults> results = result.bciComputationResult().bciResults();
        assertEquals(12, results.size());
        assertThat(results.get("N11")).hasInTargetNp(748.0);
    }

    @Test
    void runProcessWithFeasibilityRangesAndInitialNetPositions() throws IOException {

        final MergingTask task = prepareTask(2L,
                                             correctNpfPath,
                                             correctEcPath,
                                             null,
                                             correctFeasibilityRangePath,
                                             correctInitialNetPositionsPath);

        bciProcess = new BciProcess(task, configuration);
        bciProcess.run();

        final BciProcessResult result = getResult(task);
        assertEquals("CE", result.regionName());
        assertEquals(processTargetDate, result.processDateTime());
        final Map<String, BciAreaResults> results = result.bciComputationResult().bciResults();
        assertEquals(12, results.size());
        assertThat(results.get("N11")).hasInTargetNp(687.6);
    }

    @Test
    void errorWithBlankArgs() {
        assertThatThrownBy(() -> prepareTask(1L, "", "", "", "", ""))
            .isValidServiceException();
    }

    @Test
    void runProcessWithAlegro() throws IOException {
        final MergingTask task = prepareTask(0L, npfWithAlegroXnodesPath, ecWithAlegroXnodesPath, alegroDataPath, null, initialNetPositionsPath);

        task.getInputs().setTargetDate(OffsetDateTime.parse("2020-06-07T22:30Z"));
        bciProcess = new BciProcess(task, configuration);
        bciProcess.run();

        final BciProcessResult result = getResult(task);
        final BciAlegroFlows albeFlows = result.bciAlegroData().albeFlows();
        final BciAlegroFlows aldeFlows = result.bciAlegroData().aldeFlows();
        assertEquals("CE", result.regionName());
        assertEquals(-200, albeFlows.targetFlow());
        assertEquals(100, albeFlows.maxEc());
        assertEquals(-100, albeFlows.minEc());
        assertEquals(200, aldeFlows.targetFlow());
        assertEquals(100, aldeFlows.maxEc());
        assertEquals(-100, aldeFlows.minEc());
    }

    private BciProcessResult getResult(final MergingTask task) throws FileNotFoundException {
        return BciResultUtil.read(
            new FileInputStream(task.getArtifactPath(BCI_OUTPUT_FILE))
        );
    }

    private MergingTask prepareTask(final Long id,
                                    final String npfPath,
                                    final String ecPath,
                                    final String alegroPath,
                                    final String feasibilityRangePath,
                                    final String initialNetPositionsPath) throws IOException {
        final MergingTask task = new MergingTask();
        task.setId(id);
        task.setArtifacts(new Artifacts());
        task.setInputs(new Inputs());
        task.setConfigurations(new Configurations());
        createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        createDirectories(Paths.get(configuration.getInputsDirectoryPath(task)));

        if (npfPath != null) {
            task.setArtifact(
                REFERENCE_PROGRAM_FORECAST_FILE,
                new SavedFile("forecastReferenceProgram.json", npfPath, "netPositionForecast")
            );
        }

        if (initialNetPositionsPath != null) {
            task.setArtifact(
                IGMS_NET_POSITIONS_FILE,
                new SavedFile("igmsNetPositions.json", initialNetPositionsPath, "igms-net-position")
            );
        }

        if (alegroPath != null) {
            task.setArtifact(
                ALEGRO_NET_POSITIONS,
                new SavedFile("alegroNetPositions.json", alegroPath, "igms-net-position")
            );
        }

        if (ecPath != null) {
            task.getInputs().setExternalConstraintsFilePath(ecPath);
        }

        if (feasibilityRangePath != null) {
            task.getInputs().setFeasibilityRangesFilePath(feasibilityRangePath);
        }

        task.getInputs().setTargetDate(processTargetDate);
        task.getConfigurations().setRegionConfiguration(regionConfiguration);
        return task;
    }

}

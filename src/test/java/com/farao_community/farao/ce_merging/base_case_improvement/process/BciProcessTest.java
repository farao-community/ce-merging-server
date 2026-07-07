/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.data.result.BciProcessResult;
import com.farao_community.farao.ce_merging.base_case_improvement.data.result.JsonBciResult;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciInputs;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciOutput;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciTask;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.CeTestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.stringPathOf;

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
    private CeMergingConfiguration bciConfiguration;

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

    @Test
    void runProcessWithoutFeasibilityRanges() throws IOException {
        BciTask task = new BciTask();
        setTaskId(task, 0L);
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task)));
        BciInputs bciInputs = new BciInputs(correctNpfPath, correctEcPath);
        task.setBciInputs(bciInputs);
        BciOutput output = new BciOutput(bciConfiguration.getBciOutputsDirectoryPath(task));
        task.setBciOutput(output);
        task.setRegionConfiguration(jsonEicConfig);
        task.setProcessTargetDate(processTargetDate);
        bciProcess = new BciProcess(task, bciConfiguration, regionConfiguration);
        bciProcess.run();

        Path resultsPath = Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task) + File.separator + "bciOutput.json");
        assertTrue(resultsPath.toFile().exists());

        BciProcessResult bciProcessResult = JsonBciResult.read(new FileInputStream(resultsPath.toFile()));
        assertEquals("CE", bciProcessResult.getRegionName());
        assertEquals(processTargetDate, bciProcessResult.getProcessDateTime());
        assertEquals(12, bciProcessResult.getBciComputationResult().getBciResults().size());
        assertEquals(702., bciProcessResult.getBciComputationResult().getBciResults().get("N11").getInRegionNetPositions().getTarget(), 0.1);
    }

    @Test
    void runProcessWithAbsoluteFeasibilityRanges() throws IOException {
        BciTask task = new BciTask();
        setTaskId(task, 1L);
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task)));
        BciInputs bciInputs = new BciInputs(correctNpfPath, correctEcPath);
        bciInputs.setFeasibilityRangePath(correctFeasibilityRangePathAbsolute);
        task.setBciInputs(bciInputs);
        BciOutput output = new BciOutput(bciConfiguration.getBciOutputsDirectoryPath(task));
        task.setBciOutput(output);
        task.setProcessTargetDate(processTargetDate);
        task.setRegionConfiguration(jsonEicConfig);
        bciProcess = new BciProcess(task, bciConfiguration, regionConfiguration);
        bciProcess.run();

        Path resultsPath = Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task) + File.separator + "bciOutput.json");
        assertTrue(resultsPath.toFile().exists());

        BciProcessResult bciProcessResult = JsonBciResult.read(new FileInputStream(resultsPath.toFile()));
        assertEquals("CE", bciProcessResult.getRegionName());
        assertEquals(processTargetDate, bciProcessResult.getProcessDateTime());
        assertEquals(12, bciProcessResult.getBciComputationResult().getBciResults().size());
        assertEquals(748.0, bciProcessResult.getBciComputationResult().getBciResults().get("N11").getInRegionNetPositions().getTarget(), 0.1);
    }

    @Test
    void runProcessWithFeasibilityRangesAndInitialNetPositions() throws IOException {
        BciTask task = new BciTask();
        setTaskId(task, 2L);
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task)));
        BciInputs bciInputs = new BciInputs(correctNpfPath, correctEcPath, correctFeasibilityRangePath, correctInitialNetPositionsPath);
        task.setBciInputs(bciInputs);
        BciOutput output = new BciOutput(bciConfiguration.getBciOutputsDirectoryPath(task));
        task.setBciOutput(output);
        task.setProcessTargetDate(processTargetDate);
        task.setRegionConfiguration(jsonEicConfig);
        bciProcess = new BciProcess(task, bciConfiguration, regionConfiguration);
        bciProcess.run();

        Path resultsPath = Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task) + File.separator + "bciOutput.json");
        assertTrue(resultsPath.toFile().exists());

        BciProcessResult bciProcessResult = JsonBciResult.read(new FileInputStream(resultsPath.toFile()));
        assertEquals("CE", bciProcessResult.getRegionName());
        assertEquals(processTargetDate, bciProcessResult.getProcessDateTime());
        assertEquals(12, bciProcessResult.getBciComputationResult().getBciResults().size());
        assertEquals(687.6, bciProcessResult.getBciComputationResult().getBciResults().get("N11").getInRegionNetPositions().getTarget(), 0.1);
    }

    @Test
    void runProcessWithError() {
        BciTask task = new BciTask();
        setTaskId(task, 1L);
        BciInputs bciInputs = new BciInputs("", "");
        task.setBciInputs(bciInputs);
        bciProcess = new BciProcess(task, bciConfiguration, regionConfiguration);
        try {
            bciProcess.run();
        } catch (CeMergingException e) {
            //should throw exception
        }
    }

    @Test
    void runProcessWithAlegro() throws IOException {
        BciTask task = new BciTask();
        setTaskId(task, 0L);
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task)));
        BciInputs bciInputs = new BciInputs(npfWithAlegroXnodesPath, ecWithAlegroXnodesPath);
        bciInputs.setAlegroNetPositionsPath(alegroDataPath);
        bciInputs.setInitialNetPositionsPath(initialNetPositionsPath);
        task.setBciInputs(bciInputs);

        BciOutput output = new BciOutput(bciConfiguration.getBciOutputsDirectoryPath(task));
        task.setBciOutput(output);
        task.setProcessTargetDate(OffsetDateTime.parse("2020-06-07T22:30Z"));
        task.setRegionConfiguration(jsonEicConfig);
        bciProcess = new BciProcess(task, bciConfiguration, regionConfiguration);
        bciProcess.run();
        Path resultsPath = Paths.get(bciConfiguration.getBciOutputsDirectoryPath(task) + File.separator + "bciOutput.json");
        assertTrue(resultsPath.toFile().exists());
        BciProcessResult bciProcessResult = JsonBciResult.read(new FileInputStream(resultsPath.toFile()));
        assertEquals("CE", bciProcessResult.getRegionName());
        assertEquals(-200, bciProcessResult.getBciAlegroData().getAlbeFlows().getTargetFlow(), 0.);
        assertEquals(100, bciProcessResult.getBciAlegroData().getAlbeFlows().getMaxEc(), 0.);
        assertEquals(-100, bciProcessResult.getBciAlegroData().getAlbeFlows().getMinEc(), 0.);
        assertEquals(200, bciProcessResult.getBciAlegroData().getAldeFlows().getTargetFlow(), 0.);
        assertEquals(100, bciProcessResult.getBciAlegroData().getAldeFlows().getMaxEc(), 0.);
        assertEquals(-100, bciProcessResult.getBciAlegroData().getAldeFlows().getMinEc(), 0.);
    }

    private void setTaskId(BciTask task, long id) {
        try {
            Field idField = task.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(task, id);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            // Should not happen
        }
    }
}

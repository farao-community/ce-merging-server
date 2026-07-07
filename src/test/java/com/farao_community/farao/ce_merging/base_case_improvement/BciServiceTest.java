/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement;

import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciInputs;
import com.farao_community.farao.ce_merging.base_case_improvement.data.task.BciTask;
import com.farao_community.farao.ce_merging.base_case_improvement.repository.BciTaskRepository;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;
import test_utils.CeTestUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.farao_community.farao.ce_merging.common.task.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.common.task.TaskStatus.SUCCESS;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class BciServiceTest {
    @Autowired
    private BciService service;

    @Autowired
    CeMergingConfiguration bciConfiguration;

    private BciTaskRepository bciTaskRepository;
    private RegionConfiguration regionConfiguration;

    private MultipartFile correctForecastNpFile;

    private MultipartFile correctEcFile;

    private MultipartFile correctFeasibilityRange;

    private MultipartFile correctInitialNpFile;

    private MultipartFile eicConfigMultipartFile;

    private BciTask bciTaskCreated;
    private BciTask bciTaskRunning;
    private BciTask bciTaskSuccess;

    private OffsetDateTime processTargetDate = OffsetDateTime.now();

    @BeforeEach
    void setUp() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        bciTaskRepository = Mockito.mock(BciTaskRepository.class);
        File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        String jsonConfig = new String(readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);

        correctForecastNpFile = new MockMultipartFile("NPF_example.xml", getClass().getResourceAsStream("/bci/NPF_example.xml"));
        correctEcFile = new MockMultipartFile("F229-MergedECs_v01.xml", getClass().getResourceAsStream("/bci/F229-MergedECs_v01.xml"));
        correctFeasibilityRange = new MockMultipartFile("bciFeasibilityRange.xml", getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
        correctInitialNpFile = new MockMultipartFile("20190618_initialNetPositions.json", getClass().getResourceAsStream("/bci/20190618_initialNetPositions.json"));
        eicConfigMultipartFile = new MockMultipartFile("region_configuration.json", getClass().getResourceAsStream("/bci/region_configuration.json"));

        service = new BciService(bciConfiguration, bciTaskRepository);

        bciTaskCreated = new BciTask();
        bciTaskCreated.setId(0L);
        bciTaskCreated.setName("first task");
        bciTaskCreated.setStatus(CREATED);
        bciTaskCreated.setProcessTargetDate(OffsetDateTime.now());

        when(bciTaskRepository.save(any())).thenReturn(bciTaskCreated);

        BciInputs bciInputs = new BciInputs();
        bciInputs.setFeasibilityRangePath(bciTaskCreated + File.separator + "feasibility_range_location");
        bciInputs.setExternalConstraintsPath(bciTaskCreated + File.separator + "ec_location");
        bciInputs.setForecastNetPositionsPath(bciTaskCreated + File.separator + "npf_location");
        bciInputs.setInitialNetPositionsPath(bciTaskCreated + File.separator + "init_np_location");
        bciTaskCreated.setBciInputs(bciInputs);

        bciTaskRunning = new BciTask();
        bciTaskRunning.setId(1L);
        bciTaskRunning.setName("second task");
        bciTaskRunning.setStatus(RUNNING);
        bciTaskRunning.setProcessTargetDate(OffsetDateTime.now());

        BciInputs bciInputsRunning = new BciInputs();
        bciInputsRunning.setFeasibilityRangePath(bciTaskRunning + File.separator + "feasibility_range_location");
        bciInputsRunning.setExternalConstraintsPath(bciTaskRunning + File.separator + "ec_location");
        bciInputsRunning.setForecastNetPositionsPath(bciTaskRunning + File.separator + "npf_location");
        bciInputs.setInitialNetPositionsPath(bciTaskRunning + File.separator + "init_np_location");
        bciTaskRunning.setBciInputs(bciInputsRunning);

        bciTaskSuccess = new BciTask();
        bciTaskSuccess.setId(2L);
        bciTaskSuccess.setName("success task");
        bciTaskSuccess.setStatus(SUCCESS);
        bciTaskSuccess.setProcessTargetDate(OffsetDateTime.now());

        // Create results directory and fill it for successful task
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskCreated)));
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskRunning)));
        createDirectories(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskSuccess)));

        when(bciTaskRepository.findById(0L)).thenReturn(Optional.of(bciTaskCreated));
        when(bciTaskRepository.findById(1L)).thenReturn(Optional.of(bciTaskRunning));
        when(bciTaskRepository.findById(2L)).thenReturn(Optional.of(bciTaskSuccess));
    }

    @AfterEach
    void teardown() throws IOException {
        FileSystemUtils.deleteRecursively(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskCreated)));
        FileSystemUtils.deleteRecursively(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskRunning)));
        FileSystemUtils.deleteRecursively(Paths.get(bciConfiguration.getBciOutputsDirectoryPath(bciTaskSuccess)));
    }

    @Test
    void getBciTask() {
        when(bciTaskRepository.findById(this.bciTaskCreated.getId())).thenReturn(Optional.of(bciTaskCreated));
        BciTask bciTask = service.getBciTask(0L);

        assertEquals(bciTaskCreated, bciTask);
    }

    @Test
    void getAllBciTask() {
        List<BciTask> bciTasks = new ArrayList<>();
        bciTasks.add(bciTaskCreated);
        when(bciTaskRepository.findAll()).thenReturn(bciTasks);
        List<BciTask> results = service.getAllBciTasks();

        assertEquals(0L, results.getFirst().getId());
    }

    @Test
    void createBciTask() {
        BciTask bciTask = service.createBciTask("name", processTargetDate, correctForecastNpFile, correctEcFile, Optional.ofNullable(correctFeasibilityRange), Optional.ofNullable(correctInitialNpFile), Optional.empty(), Optional.ofNullable(eicConfigMultipartFile));

        assertEquals("name", bciTask.getName());
        assertEquals(CREATED, bciTask.getStatus());
        assertEquals(0L, bciTask.getId());
        assertEquals(processTargetDate, bciTask.getProcessTargetDate());

    }

    @Test
    void createBciTaskWithoutFeasibilityRange() {
        BciTask bciTask = service.createBciTask("name", processTargetDate, correctForecastNpFile, correctEcFile, Optional.empty(), Optional.ofNullable(correctInitialNpFile), Optional.empty(), Optional.ofNullable(eicConfigMultipartFile));

        assertEquals("name", bciTask.getName());
        assertEquals(CREATED, bciTask.getStatus());
        assertEquals(0L, bciTask.getId());
        assertEquals(processTargetDate, bciTask.getProcessTargetDate());
        assertNull(bciTask.getBciInputs().getFeasibilityRangePath());
    }

    @Test
    void createBciTaskWithoutInitialNetPositions() {
        BciTask bciTask = service.createBciTask("name", processTargetDate, correctForecastNpFile, correctEcFile, Optional.ofNullable(correctFeasibilityRange), Optional.empty(), Optional.empty(), Optional.ofNullable(eicConfigMultipartFile));

        assertEquals("name", bciTask.getName());
        assertEquals(CREATED, bciTask.getStatus());
        assertEquals(0L, bciTask.getId());
        assertEquals(processTargetDate, bciTask.getProcessTargetDate());
        assertNull(bciTask.getBciInputs().getInitialNetPositionsPath());
    }

    @Test
    void createBciTaskWithoutOptionalFiles() {
        BciTask bciTask = service.createBciTask("name", processTargetDate, correctForecastNpFile, correctEcFile, Optional.empty(), Optional.empty(), Optional.empty(), Optional.ofNullable(eicConfigMultipartFile));

        assertEquals("name", bciTask.getName());
        assertEquals(CREATED, bciTask.getStatus());
        assertEquals(0L, bciTask.getId());
        assertEquals(processTargetDate, bciTask.getProcessTargetDate());
        assertNull(bciTask.getBciInputs().getInitialNetPositionsPath());
        assertNull(bciTask.getBciInputs().getFeasibilityRangePath());
    }

    @Test
    void testRemoveBciTask() {
        assertDoesNotThrow(() -> service.removeBciTask(0L));
    }

    @Test
    void getInputsTasks() throws IOException {
        BciTask bciTask = service.createBciTask("name", processTargetDate, correctForecastNpFile, correctEcFile, Optional.ofNullable(correctFeasibilityRange), Optional.ofNullable(correctInitialNpFile), Optional.empty(), Optional.ofNullable(eicConfigMultipartFile));
        byte[] result = service.getInputZip(bciTask.getId());
        ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(result)));
        ZipEntry entry = zipInputStream.getNextEntry();

        boolean npfFileFound = false;

        while (zipInputStream.available() > 0) {
            if (entry.getName().contains("NPF_example.xml")) {
                npfFileFound = true;
            }
            try {
                entry = zipInputStream.getNextEntry();
            } catch (EOFException e) {
                break;
            }
        }
        assertTrue(npfFileFound);
    }
}

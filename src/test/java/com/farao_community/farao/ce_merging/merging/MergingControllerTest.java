/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.common.util.FileUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;

import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static test_utils.CeTestUtils.ID_1;
import static test_utils.CeTestUtils.ID_2;
import static test_utils.CeTestUtils.MIME_ZIP;
import static test_utils.CeTestUtils.INPUTS_ZIP_NAME;
import static test_utils.CeTestUtils.taskDtoWithIdAndStatus;
import static test_utils.CeTestUtils.anyFile;
import static test_utils.CeTestUtils.stringContentOf;
import static test_utils.CeTestUtils.byteContentOf;
import static test_utils.CeTestUtils.METADATA;
import static test_utils.CeTestUtils.INPUTS;

class MergingControllerTest {

    private final MergingTaskManagementService taskManager = mock(MergingTaskManagementService.class);
    private final MergingController controller = new MergingController(taskManager);

    @Test
    void shouldGetOutputsAsAttachments() {
        final MergingTask task = mock(MergingTask.class);
        when(task.getStatus()).thenReturn(SUCCESS);
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.getTaskById(ID_1))
                .thenReturn(task);
        when(taskManager.getCgm(ID_1))
                .thenReturn(new SavedFile());
        when(taskManager.getCgmNetPositions(ID_1))
                .thenReturn(new SavedFile());
        when(taskManager.getOutputZip(ID_1))
                .thenReturn(new byte[0]);
        when(taskManager.getRefProg(ID_1))
                .thenReturn(new SavedFile());
        when(taskManager.getXnodesInformation(ID_1))
                .thenReturn(new SavedFile());

        try (final MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            controller.getCgmOutput(ID_1);
            controller.getOutputsByTaskId(ID_1);
            controller.getRefProgOutput(ID_1);

            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(anyFile()),
                             times(2));
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(any(), anyString()));
        }
    }

    @Test
    void shouldNotGetOutputsAsAttachmentsIfTaskUnknown() {
        when(taskManager.checkTaskExist(ID_2))
                .thenReturn(false);
        final ResponseEntity<byte[]> cgmResponse = controller.getCgmOutput(ID_2);
        final ResponseEntity<byte[]> outputsREsponse = controller.getOutputsByTaskId(ID_2);
        final ResponseEntity<byte[]> refprogResponse = controller.getRefProgOutput(ID_2);
        Assertions.assertThat(cgmResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(outputsREsponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(refprogResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldNotGetOutputsAsAttachmentsIfBadRequest() {
        final MergingTask task = mock(MergingTask.class);
        when(task.getStatus()).thenReturn(RUNNING);
        when(taskManager.checkTaskExist(ID_2))
                .thenReturn(true);
        when(taskManager.getTaskById(ID_2))
                .thenReturn(task);
        final ResponseEntity<byte[]> cgmResponse = controller.getCgmOutput(ID_2);
        final ResponseEntity<byte[]> outputsREsponse = controller.getOutputsByTaskId(ID_2);
        final ResponseEntity<byte[]> refprogResponse = controller.getRefProgOutput(ID_2);
        Assertions.assertThat(cgmResponse.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(outputsREsponse.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(refprogResponse.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void shouldGetInputsAsAttachments() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.getInputsZip(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getIgm(ID_1, "FR")).thenReturn(new SavedFile());
        when(taskManager.getIgmQualityReport(ID_1, "FR")).thenReturn(new SavedFile());
        when(taskManager.getGenerationLoadShiftKeys(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getExternalConstraints(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getFeasibilityRanges(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getDcLinks(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getNetPositionForecast(ID_1)).thenReturn(new SavedFile());

        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {

            controller.getInputs(ID_1);
            controller.getIgm(ID_1, "FR");
            controller.getIgmQualityReport(ID_1, "FR");
            controller.getGenerationLoadShiftKeys(ID_1);
            controller.getExternalConstraints(ID_1);
            controller.getFeasibilityRanges(ID_1);
            controller.getDcLinks(ID_1);
            controller.getNetPositionForecast(ID_1);

            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(any(), anyString()));
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(anyFile()), times(7));
        }
    }

    @Test
    void shouldNotGetInputsAsAttachmentsBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);
        when(taskManager.getInputsZip(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getIgm(ID_1, "FR")).thenReturn(new SavedFile());
        when(taskManager.getIgmQualityReport(ID_1, "FR")).thenReturn(new SavedFile());
        when(taskManager.getGenerationLoadShiftKeys(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getExternalConstraints(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getFeasibilityRanges(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getDcLinks(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getNetPositionForecast(ID_1)).thenReturn(new SavedFile());

        final ResponseEntity<byte[]> inputs = controller.getInputs(ID_1);
        final ResponseEntity<byte[]> igm = controller.getIgm(ID_1, "FR");
        final ResponseEntity<byte[]> igmQualityReport = controller.getIgmQualityReport(ID_1, "FR");
        final ResponseEntity<byte[]> generationLoadShiftKeys = controller.getGenerationLoadShiftKeys(ID_1);
        final ResponseEntity<byte[]> externalConstraints = controller.getExternalConstraints(ID_1);
        final ResponseEntity<byte[]> feasibilityRanges = controller.getFeasibilityRanges(ID_1);
        final ResponseEntity<byte[]> dcLinks = controller.getDcLinks(ID_1);
        final ResponseEntity<byte[]> netPositionForecast = controller.getNetPositionForecast(ID_1);
        Assertions.assertThat(inputs.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(igm.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(igmQualityReport.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(generationLoadShiftKeys.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(externalConstraints.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(feasibilityRanges.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(dcLinks.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(netPositionForecast.getStatusCode()).isEqualTo(NOT_FOUND);

    }

    @Test
    void shouldCreateTask() {
        try {
            // necessary for MvcUriComponentsBuilder
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setContextPath("/test");
            final ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);
            RequestContextHolder.setRequestAttributes(attrs);
            //

            final MergingTaskDto task = taskDtoWithIdAndStatus(1, CREATED);
            when(taskManager.createNewTask(any(MultipartFile.class), anyString()))
                    .thenReturn(task);

            final MockMultipartFile inputZip = new MockMultipartFile(INPUTS_ZIP_NAME,
                                                                     INPUTS_ZIP_NAME,
                                                                     MIME_ZIP,
                                                                     byteContentOf(INPUTS));

            final ResponseEntity<JsonApiDocument<MergingTaskDto>> response = controller.createTask(
                    inputZip, stringContentOf(METADATA)
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());

            assertThat(response.getHeaders().getLocation())
                    .hasPath("/test/ce-merging/v1/tasks/1");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }

    }

    @Test
    void shouldGetTask() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        final MergingTaskDto task = taskDtoWithIdAndStatus(ID_1, ERROR);
        when(taskManager.getTaskJsonDoc(ID_1))
                .thenReturn(JsonApiDocument.fromData(task));

        assertTaskIsInOkResponse(task, controller.getTask(ID_1));
    }

    @Test
    void shouldNotGetTaskBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);

        final ResponseEntity<JsonApiDocument<MergingTaskDto>> task = controller.getTask(ID_1);

        Assertions.assertThat(task.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldRunTask() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        final MergingTaskDto task = taskDtoWithIdAndStatus(ID_1, SUCCESS);
        when(taskManager.runTask(ID_1))
                .thenReturn(task);

        assertTaskIsInOkResponse(task, controller.runTask(ID_1));
    }

    @Test
    void shouldNotRunTaskBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);

        final ResponseEntity<JsonApiDocument<MergingTaskDto>> runTaskResponse = controller.runTask(ID_1);
        Assertions.assertThat(runTaskResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldNotRunTaskBecauseRunning() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.runTask(ID_1))
                .thenThrow(new TaskAlreadyRunningException("already running"));

        final ResponseEntity<JsonApiDocument<MergingTaskDto>> runTaskResponse = controller.runTask(ID_1);
        Assertions.assertThat(runTaskResponse.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    @Test
    void shouldListTasks() {
        final List<MergingTaskDto> tasks = List.of(
                taskDtoWithIdAndStatus(ID_1, CREATED),
                taskDtoWithIdAndStatus(ID_2, SUCCESS)
        );
        when(taskManager.getAllTasks()).thenReturn(JsonApiDocument.fromDataList(tasks));
        final ResponseEntity<JsonApiDocument<MergingTaskDto>> response = controller.listTasks();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(tasks, response.getBody().data);
        verify(taskManager).getAllTasks();
    }

    @Test
    void shouldDeleteTask() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        final ResponseEntity<Void> response = controller.deleteTask(ID_1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManager).deleteTask(ID_1);
    }

    @Test
    void shouldNotDeleteTaskBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);
        final ResponseEntity<Void> response = controller.deleteTask(ID_1);
        assertEquals(NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldDeleteAllTasks() {
        final ResponseEntity<Void> response = controller.deleteAllTasks();
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManager).deleteAllTasks();
    }

    @Test
    void shouldPublishGlobalConfigurations() {
        MockMultipartFile file = new MockMultipartFile(
                "configurationFile",
                "config.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusDays(1);
        controller.publishVirtualHubsConfiguration(file, from, to);
        controller.publishXNodesConfiguration(file, from, to);
        controller.publishVirtualHubsAlignmentConfiguration(file, from, to);
        controller.publishBECKeyConfiguration(file, from, to);
        controller.publishEICCodeConfiguration(file, from, to);
        verify(taskManager).publishVirtualHubsConfiguration(file, from, to);
        verify(taskManager).publishXNodesConfiguration(file, from, to);
        verify(taskManager).publishHvdcXNodeAlignmentConfiguration(file, from, to);
        verify(taskManager).publishBECKeyConfiguration(file, from, to);
        verify(taskManager).publishRegionConfiguration(file, from, to);
    }

    @Test
    void shouldNotPublishGlobalConfigurations() {
        MockMultipartFile file = new MockMultipartFile(
                "configurationFile",
                "config.json",
                MediaType.APPLICATION_JSON_VALUE,
                new byte[0]
        );

        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusDays(1);
        doThrow(new CeMergingException("test")).when(taskManager).publishVirtualHubsConfiguration(file, from, to);
        doThrow(new CeMergingException("test")).when(taskManager).publishXNodesConfiguration(file, from, to);
        doThrow(new CeMergingException("test")).when(taskManager).publishHvdcXNodeAlignmentConfiguration(file, from, to);
        doThrow(new CeMergingException("test")).when(taskManager).publishBECKeyConfiguration(file, from, to);
        doThrow(new CeMergingException("test")).when(taskManager).publishRegionConfiguration(file, from, to);

        final ResponseEntity<byte[]> publishVirtualHubsResponse = controller.publishVirtualHubsConfiguration(file, from, to);
        final ResponseEntity<byte[]> publishXNodesResponse = controller.publishXNodesConfiguration(file, from, to);
        final ResponseEntity<byte[]> publishVirtualHubsAlignmentHubsResponse = controller.publishVirtualHubsAlignmentConfiguration(file, from, to);
        final ResponseEntity<byte[]> publishBECKeyResponse = controller.publishBECKeyConfiguration(file, from, to);
        final ResponseEntity<byte[]> publishEICCodeResponse = controller.publishEICCodeConfiguration(file, from, to);

        Assertions.assertThat(publishVirtualHubsResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(publishXNodesResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(publishVirtualHubsAlignmentHubsResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(publishBECKeyResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(publishEICCodeResponse.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldGetGlobalConfigurationsAsAttachments() throws Exception {
        when(taskManager.getVirtualHubsConfiguration(any())).thenReturn(new byte[0]);
        when(taskManager.getXNodesConfiguration(any())).thenReturn(new byte[0]);
        when(taskManager.getHvdcXNodeAlignmentConfiguration(any())).thenReturn(new byte[0]);
        when(taskManager.getBECKeyConfiguration(any())).thenReturn(new byte[0]);
        when(taskManager.getRegionConfiguration(any())).thenReturn(new byte[0]);
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            controller.getVirtualHubsConfiguration(null);
            controller.getXNodesConfiguration(null);
            controller.getHvdcXnodeAlignementConfiguration(null);
            controller.getBecConfiguration(null);
            controller.getEICConfiguration(null);
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(any(), anyString()), times(5));
        }
    }

    @Test
    void shouldGetTaskConfigurationsAsAttachments() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.getDcLoadFlowParameters(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getAcLoadFlowParameters(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getBasecaseImprovementParameters(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getBalancesAdjustmentParameters(ID_1)).thenReturn(new SavedFile());
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            controller.getDcLoadFlowParameters(ID_1);
            controller.getAcLoadFlowParameters(ID_1);
            controller.getBasecaseImprovementParameters(ID_1);
            controller.getBalancesAdjustmenParameters(ID_1);
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(anyFile()), times(4));
        }
    }

    @Test
    void shouldNotGetTaskConfigurationsAsAttachmentsBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);
        final ResponseEntity<byte[]> dcLoadFlowResponse = controller.getDcLoadFlowParameters(ID_1);
        final ResponseEntity<byte[]> acLoadFlowResponse = controller.getAcLoadFlowParameters(ID_1);
        final ResponseEntity<byte[]> basecaseResponse = controller.getBasecaseImprovementParameters(ID_1);
        final ResponseEntity<byte[]> balancesAdjustmen = controller.getBalancesAdjustmenParameters(ID_1);

        Assertions.assertThat(dcLoadFlowResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(acLoadFlowResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(basecaseResponse.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(balancesAdjustmen.getStatusCode()).isEqualTo(NOT_FOUND);

    }

    @Test
    void shouldGetArtifactsAsAttachments() {
        final MergingTask task = mock(MergingTask.class);
        when(task.getStatus()).thenReturn(SUCCESS);
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.getTaskById(ID_1))
                .thenReturn(task);
        when(taskManager.getArtifactsZip(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getGermanPreMerge(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getDkConverted(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getTopologicalMerge(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getCgmAfterRecessivity(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getCgmAfterPstSpecialProcedure(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getActualGlskReport(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getActualGlskCorrected(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getIgmsNetPositions(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getGermanIgmsNetPositions(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getBciOutput(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getBalancesAdjustmentTarget(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getCgmNetPositions(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getTgmNetPositions(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getAlegroNetPositions(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getBalancedCgm(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getPstOutput(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getExecutionLogs(ID_1)).thenReturn(new byte[0]);
        when(taskManager.getOpenLoadFlowLogs(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getXnodesInformation(ID_1)).thenReturn(new SavedFile());
        when(taskManager.getXnodesInconsistencies(ID_1)).thenReturn(new SavedFile());
        try (MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            controller.getArtifacts(ID_1);
            controller.getGermanPreMerge(ID_1);
            controller.getDkConverted(ID_1);
            controller.getTopologicalMerge(ID_1);
            controller.getCgmAfterRecessivity(ID_1);
            controller.getCgmAfterPstSpecialProcedure(ID_1);
            controller.getActualGlskQualityReport(ID_1);
            controller.getActualGlskCorrected(ID_1);
            controller.getIgmsNetPositions(ID_1);
            controller.getGermanIgmsNetPositions(ID_1);
            controller.getBciOutput(ID_1);
            controller.getBalancesAdjustmentTarget(ID_1);
            controller.getCgmNetPositions(ID_1);
            controller.getTgmNetPositions(ID_1);
            controller.getAlegroNetPositions(ID_1);
            controller.getBalancedCgm(ID_1);
            controller.getPstResult(ID_1);
            controller.getExecutionLogs(ID_1);
            controller.getOpenLoadFlowLogs(ID_1);
            controller.getXnodesInformation(ID_1);
            controller.getXnodesInconsistencies(ID_1);
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(anyFile()), times(14));
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(any(), anyString()), times(7));
        }
    }

    @Test
    void shouldNotGetArtifactsAsAttachmentsBecauseNotFound() {
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(false);
        final ResponseEntity<byte[]> artifacts = controller.getArtifacts(ID_1);
        final ResponseEntity<byte[]> germanPreMerge = controller.getGermanPreMerge(ID_1);
        final ResponseEntity<byte[]> dkConverted = controller.getDkConverted(ID_1);
        final ResponseEntity<byte[]> topologicalMerge = controller.getTopologicalMerge(ID_1);
        final ResponseEntity<byte[]> cgmAfterRecessivity = controller.getCgmAfterRecessivity(ID_1);
        final ResponseEntity<byte[]> cgmAfterPstSpecialProcedure = controller.getCgmAfterPstSpecialProcedure(ID_1);
        final ResponseEntity<byte[]> actualGlskQualityReport = controller.getActualGlskQualityReport(ID_1);
        final ResponseEntity<byte[]> actualGlskCorrected = controller.getActualGlskCorrected(ID_1);
        final ResponseEntity<byte[]> igmsNetPositions = controller.getIgmsNetPositions(ID_1);
        final ResponseEntity<byte[]> germanIgmsNetPositions = controller.getGermanIgmsNetPositions(ID_1);
        final ResponseEntity<byte[]> bciOutput = controller.getBciOutput(ID_1);
        final ResponseEntity<byte[]> balancesAdjustmentTarget = controller.getBalancesAdjustmentTarget(ID_1);
        final ResponseEntity<byte[]> cgmNetPositions = controller.getCgmNetPositions(ID_1);
        final ResponseEntity<byte[]> tgmNetPositions = controller.getTgmNetPositions(ID_1);
        final ResponseEntity<byte[]> alegroNetPositions = controller.getAlegroNetPositions(ID_1);
        final ResponseEntity<byte[]> balancedCgm = controller.getBalancedCgm(ID_1);
        final ResponseEntity<byte[]> pstResult = controller.getPstResult(ID_1);
        final ResponseEntity<byte[]> executionLogs = controller.getExecutionLogs(ID_1);
        final ResponseEntity<byte[]> openLoadFlowLogs = controller.getOpenLoadFlowLogs(ID_1);
        final ResponseEntity<byte[]> xnodesInformation = controller.getXnodesInformation(ID_1);
        final ResponseEntity<byte[]> xnodesInconsistencies = controller.getXnodesInconsistencies(ID_1);

        Assertions.assertThat(artifacts.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(germanPreMerge.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(dkConverted.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(topologicalMerge.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(cgmAfterRecessivity.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(cgmAfterPstSpecialProcedure.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(actualGlskQualityReport.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(actualGlskCorrected.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(igmsNetPositions.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(germanIgmsNetPositions.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(bciOutput.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(balancesAdjustmentTarget.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(cgmNetPositions.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(tgmNetPositions.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(alegroNetPositions.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(balancedCgm.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(pstResult.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(executionLogs.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(openLoadFlowLogs.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(xnodesInformation.getStatusCode()).isEqualTo(NOT_FOUND);
        Assertions.assertThat(xnodesInconsistencies.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void shouldNotGetArtifactsAsAttachmentsBecauseBadRequest() {

        final MergingTask task = mock(MergingTask.class);
        when(task.getStatus()).thenReturn(CREATED);
        when(taskManager.checkTaskExist(ID_1))
                .thenReturn(true);
        when(taskManager.getTaskById(ID_1))
                .thenReturn(task);
        final ResponseEntity<byte[]> germanPreMerge = controller.getGermanPreMerge(ID_1);
        final ResponseEntity<byte[]> dkConverted = controller.getDkConverted(ID_1);
        final ResponseEntity<byte[]> topologicalMerge = controller.getTopologicalMerge(ID_1);
        final ResponseEntity<byte[]> cgmAfterRecessivity = controller.getCgmAfterRecessivity(ID_1);
        final ResponseEntity<byte[]> cgmAfterPstSpecialProcedure = controller.getCgmAfterPstSpecialProcedure(ID_1);
        final ResponseEntity<byte[]> actualGlskQualityReport = controller.getActualGlskQualityReport(ID_1);
        final ResponseEntity<byte[]> actualGlskCorrected = controller.getActualGlskCorrected(ID_1);
        final ResponseEntity<byte[]> igmsNetPositions = controller.getIgmsNetPositions(ID_1);
        final ResponseEntity<byte[]> germanIgmsNetPositions = controller.getGermanIgmsNetPositions(ID_1);
        final ResponseEntity<byte[]> bciOutput = controller.getBciOutput(ID_1);
        final ResponseEntity<byte[]> balancesAdjustmentTarget = controller.getBalancesAdjustmentTarget(ID_1);
        final ResponseEntity<byte[]> cgmNetPositions = controller.getCgmNetPositions(ID_1);
        final ResponseEntity<byte[]> tgmNetPositions = controller.getTgmNetPositions(ID_1);
        final ResponseEntity<byte[]> alegroNetPositions = controller.getAlegroNetPositions(ID_1);
        final ResponseEntity<byte[]> balancedCgm = controller.getBalancedCgm(ID_1);
        final ResponseEntity<byte[]> pstResult = controller.getPstResult(ID_1);
        final ResponseEntity<byte[]> executionLogs = controller.getExecutionLogs(ID_1);
        final ResponseEntity<byte[]> openLoadFlowLogs = controller.getOpenLoadFlowLogs(ID_1);
        final ResponseEntity<byte[]> xnodesInformation = controller.getXnodesInformation(ID_1);
        final ResponseEntity<byte[]> xnodesInconsistencies = controller.getXnodesInconsistencies(ID_1);

        Assertions.assertThat(germanPreMerge.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(dkConverted.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(topologicalMerge.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(cgmAfterRecessivity.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(cgmAfterPstSpecialProcedure.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(actualGlskQualityReport.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(actualGlskCorrected.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(igmsNetPositions.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(germanIgmsNetPositions.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(bciOutput.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(balancesAdjustmentTarget.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(cgmNetPositions.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(tgmNetPositions.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(alegroNetPositions.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(balancedCgm.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(pstResult.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(executionLogs.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(openLoadFlowLogs.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(xnodesInformation.getStatusCode()).isEqualTo(BAD_REQUEST);
        Assertions.assertThat(xnodesInconsistencies.getStatusCode()).isEqualTo(BAD_REQUEST);
    }

    void assertTaskIsInOkResponse(final MergingTaskDto task, final ResponseEntity<JsonApiDocument<MergingTaskDto>> response) {
        assertEquals(HttpStatus.OK,
                     response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody().data)
                .contains(task);
    }

}

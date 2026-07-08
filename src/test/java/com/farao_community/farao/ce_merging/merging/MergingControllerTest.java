/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.common.util.FileUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
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
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

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
    void shouldGetInputsAsAttachments() {
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
        final MergingTaskDto task = taskDtoWithIdAndStatus(ID_1, ERROR);
        when(taskManager.getTaskJsonDoc(ID_1))
            .thenReturn(JsonApiDocument.fromData(task));

        assertTaskIsInOkResponse(task, controller.getTask(ID_1));
    }

    @Test
    void shouldRunTask() {
        final MergingTaskDto task = taskDtoWithIdAndStatus(ID_1, SUCCESS);
        when(taskManager.runTask(ID_1))
            .thenReturn(task);

        assertTaskIsInOkResponse(task, controller.runTask(ID_1));
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
        final ResponseEntity<Void> response = controller.deleteTask(ID_1);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(taskManager).deleteTask(ID_1);
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
    void shouldGetArtifactsAsAttachments() {
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

    void assertTaskIsInOkResponse(final MergingTaskDto task, final ResponseEntity<JsonApiDocument<MergingTaskDto>> response) {
        assertEquals(HttpStatus.OK,
                     response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody().data)
            .contains(task);
    }

}

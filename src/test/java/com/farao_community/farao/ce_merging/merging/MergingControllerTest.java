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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.INPUTS;
import static test_utils.CeTestUtils.INPUTS_ZIP_NAME;
import static test_utils.CeTestUtils.METADATA;
import static test_utils.CeTestUtils.MIME_ZIP;
import static test_utils.CeTestUtils.anyFile;
import static test_utils.CeTestUtils.byteContentOf;
import static test_utils.CeTestUtils.stringContentOf;
import static test_utils.CeTestUtils.taskDtoWithIdAndStatus;

class MergingControllerTest {

    private final MergingTaskManagementService taskManager = mock(MergingTaskManagementService.class);
    private final MergingController controller = new MergingController(taskManager);
    private static MockHttpServletRequest mockRequest;

    @BeforeAll
    static void setup() {
        mockRequest = new MockHttpServletRequest();
        mockRequest.setContextPath("/test");

        final ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);

        RequestContextHolder.setRequestAttributes(attrs);
    }

    @Test
    void shouldGetOutputsAsAttachments() {
        when(taskManager.getCgm(1))
            .thenReturn(new SavedFile());
        when(taskManager.getCgmNetPositions(1))
            .thenReturn(new SavedFile());
        when(taskManager.getOutputZip(1))
            .thenReturn(new byte[0]);
        when(taskManager.getRefProg(1))
            .thenReturn(new SavedFile());
        when(taskManager.getXnodesInformation(1))
            .thenReturn(new SavedFile());

        try (final MockedStatic<FileUtils> fileUtils = mockStatic(FileUtils.class)) {
            controller.getCgmNetPositions(1);
            controller.getCgmOutput(1);
            controller.getOutputsByTaskId(1);
            controller.getRefProgOutput(1);
            controller.getXnodesInformation(1);

            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(anyFile()),
                             times(4));
            fileUtils.verify(() -> FileUtils.toAttachmentFileResponse(any(), anyString()));
        }
    }

    @Test
    void shouldCreateTask() {
        final MergingTaskDto task = taskDtoWithIdAndStatus(1, CREATED);
        when(taskManager.createNewTask(any(MultipartFile.class), anyString()))
            .thenReturn(task);

        final MockMultipartFile inputZip = new MockMultipartFile(INPUTS_ZIP_NAME,
                                                                 INPUTS_ZIP_NAME,
                                                                 MIME_ZIP,
                                                                 byteContentOf(INPUTS));

        final ResponseEntity<MergingTaskDto> response = controller.createTask(inputZip,
                                                                              stringContentOf(METADATA));

        assertEquals(HttpStatus.CREATED,
                     response.getStatusCode());

        assertThat(response.getHeaders().getLocation())
            .hasPath("/test/ce-merging/v1/tasks/1");

    }

    @Test
    void shouldGetTask() {
        final MergingTaskDto task = taskDtoWithIdAndStatus(1, ERROR);
        when(taskManager.getTaskJsonDoc(1))
            .thenReturn(JsonApiDocument.fromData(task));

        final ResponseEntity<JsonApiDocument<MergingTaskDto>> response = controller.getTask(1);

        assertEquals(HttpStatus.OK,
                     response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody().data)
            .contains(task);
    }

    @Test
    void shouldRunTask() {
        final MergingTaskDto task = taskDtoWithIdAndStatus(1, SUCCESS);
        when(taskManager.runTask(1))
            .thenReturn(task);

        final ResponseEntity<JsonApiDocument<MergingTaskDto>> response = controller.runTask(1);
        assertEquals(HttpStatus.OK,
                     response.getStatusCode());
        assertNotNull(response.getBody());
        assertThat(response.getBody().data)
            .contains(task);
    }

}

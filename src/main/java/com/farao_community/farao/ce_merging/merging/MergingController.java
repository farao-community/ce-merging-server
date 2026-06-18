/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.API_VERSION;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.BAD_REQUEST;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CE_MERGING_URL;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CREATED;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.JSON_API_MIME_TYPE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.MERGING_TASK_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NOT_FOUND;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.OK;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ORIGIN_ANY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.OUTPUTS_TAG;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TASK_MANAGEMENT_TAG;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TASK_NOT_RUN;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.toAttachmentFileResponse;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
@RestController
@CrossOrigin(origins = ORIGIN_ANY) // NOSONAR enabling CORS is safe here
@RequestMapping(value = CE_MERGING_URL + API_VERSION)
public class MergingController {

    private final MergingTaskManagementService taskManager;

    public MergingController(final MergingTaskManagementService taskManager) {
        this.taskManager = taskManager;
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                    POST
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    @PostMapping(value = "/tasks",
        consumes = {MULTIPART_FORM_DATA_VALUE},
        produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
        summary = "Create a new merging task and returns its data.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = CREATED, description = "The merging task has been created successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid merging task.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> createTask(@Parameter(description = "Input files ZIP archive")
                                                     @RequestPart final MultipartFile inputFilesArchive,
                                                     @Parameter(description = "JSON representation of the task")
                                                     @RequestPart final String inputRequestMetadata) {
        final MergingTaskDto createdTask = taskManager.createNewTask(inputFilesArchive, inputRequestMetadata);
        final Long taskId = createdTask.getTaskId();
        final UriComponents taskLocation = MvcUriComponentsBuilder.fromController(getClass())
            .path("/tasks/{taskId}")
            .buildAndExpand(taskId);

        return ResponseEntity
            .created(taskLocation.toUri())
            .body(JsonApiDocument.fromData(createdTask));
    }

    @PostMapping(value = "/tasks/{taskId}",
        produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
        summary = "Run merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The merging task has been run successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID already running."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or not reachable.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> runTask(@Parameter(description = MERGING_TASK_ID)
                                                                   @PathVariable final Long taskId) {
        return ResponseEntity.ok().body(JsonApiDocument.fromData(taskManager.runTask(taskId)));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                     GET
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}",
        produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
        summary = "Get the data of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The merging task data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or not reachable.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> getTask(@Parameter(description = MERGING_TASK_ID)
                                                                   @PathVariable final Long taskId) {
        return ResponseEntity.ok().body(taskManager.getTaskJsonDoc(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/ref-prog",
        produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
        summary = "Return the RefProg output of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The RefProg output returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or RefProg output of merging task not available")
    })
    public ResponseEntity<byte[]> getRefProgOutput(@Parameter(description = MERGING_TASK_ID)
                                                   @PathVariable final Long taskId) {
        return toAttachmentFileResponse(taskManager.getRefProg(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/cgm",
        produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
        summary = "Return the CGM output of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The CGM output returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or CGM output of merging task not available")
    })
    public ResponseEntity<byte[]> getCgmOutput(@Parameter(description = MERGING_TASK_ID)
                                               @PathVariable final Long taskId) {
        return toAttachmentFileResponse(taskManager.getCgm(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs",
        produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
        summary = "Get all the outputs of the merging task with given ID as a zip archive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The outputs archive returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found in the server, or BCI Report output of merging process task not available")
    })
    public ResponseEntity<byte[]> getOutputsByTaskId(@Parameter(description = MERGING_TASK_ID)
                                                     @PathVariable final Long taskId) {
        return toAttachmentFileResponse(taskManager.getOutputZip(taskId), "outputs_%s.zip".formatted(taskId));
    }
}

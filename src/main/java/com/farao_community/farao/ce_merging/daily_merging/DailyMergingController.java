/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.daily_merging;

import com.farao_community.farao.ce_merging.daily_merging.entities.DailyMergingTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.API_VERSION;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.BAD_REQUEST;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CE_DAILY_MERGING_URL;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CREATED;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DAILY_INPUTS_TAG;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DAILY_OUTPUTS_TAG;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DAILY_TASK_MANAGEMENT_TAG;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.JSON_API_MIME_TYPE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NOT_FOUND;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.NO_CONTENT;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.OK;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.ORIGIN_ANY;
import static com.farao_community.farao.ce_merging.common.util.FileUtils.toAttachmentFileResponse;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * WARNING: this class is used by the merging supervisor (EMERGE).
 * Please contact them to check compatibility if any modification is needed
 */
@RestController
@CrossOrigin(origins = ORIGIN_ANY) // NOSONAR enabling CORS is safe here
@RequestMapping(value = CE_DAILY_MERGING_URL + API_VERSION)
public class DailyMergingController {
    private final DailyMergingTasksManagementService dailyTasksManagementService;

    public DailyMergingController(DailyMergingTasksManagementService dailyTasksManagementService) {
        this.dailyTasksManagementService = dailyTasksManagementService;
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
               TASK MANAGEMENT
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @PostMapping(value = "/tasks",
            consumes = {MULTIPART_FORM_DATA_VALUE},
            produces = JSON_API_MIME_TYPE)
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            summary = "Creates a daily merging task from a list of task IDs.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = CREATED, description = "Daily merging taskc reated successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid merging task.")
    })
    public ResponseEntity<DailyMergingTask> createDailyTask(@Parameter(description = "List of merging task IDs") @RequestParam final Long[] taskIds,
                                                                  @Parameter(description = "Merging request file") @RequestPart final MultipartFile mergingRequest,
                                                                  @Parameter(description = "Merging version") @RequestParam final int mergingVersion) {
        final DailyMergingTask dailyCoreMergingEntity = dailyTasksManagementService.createDailyTask(Arrays.asList(taskIds), mergingRequest, mergingVersion);
        final UriComponents taskLocation = MvcUriComponentsBuilder.fromController(getClass())
                .path("/daily-merging/tasks/{taskId}")
                .buildAndExpand(dailyCoreMergingEntity.getId());
        return ResponseEntity.created(taskLocation.toUri()).body(dailyCoreMergingEntity);

    }

    @PostMapping(value = "/tasks/{taskId}",
            produces = JSON_API_MIME_TYPE)
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            description = "Run the daily merging task with the given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily merging task has been run successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<DailyMergingTask> runTask(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return ResponseEntity.ok().body(dailyTasksManagementService.runDailyMergingTask(taskId));
    }

    @GetMapping(value = "/tasks", produces = JSON_API_MIME_TYPE)
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            summary = "List existing daily merging tasks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily merging tasks list returned successfully.")
    })
    public ResponseEntity<List<DailyMergingTask>> listTasks() {
        return ResponseEntity.ok().body(dailyTasksManagementService.getAllTasks());
    }

    @GetMapping(value = "/tasks/{taskId}", produces = JSON_API_MIME_TYPE)
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            summary = "Get the data of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily merging task data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task not found.")
    })
    public ResponseEntity<DailyMergingTask> getTask(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return ResponseEntity.ok().body(dailyTasksManagementService.getTask(taskId));
    }

    @DeleteMapping(value = "/tasks/{taskId}")
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            summary = "Delete merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = NO_CONTENT, description = "Daily merging task deleted successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task with the given ID was not found.")
    })
    public ResponseEntity<Void>  deleteTask(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        dailyTasksManagementService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/tasks")
    @Operation(tags = DAILY_TASK_MANAGEMENT_TAG,
            summary = "Delete all merging tasks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = NO_CONTENT, description = "Daily merging tasks deleted successfully.")
    })
    public ResponseEntity<Void> deleteAllTasks() {
        dailyTasksManagementService.deleteAllTasks();
        return ResponseEntity.noContent().build();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                    INPUTS
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}/inputs/merging-request", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_INPUTS_TAG,
            summary = "Get the merging request input of the daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging request data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task with given ID not found.")
    })
    public ResponseEntity<byte[]>  getMergingRequest(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getMergingRequest(taskId));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                 OUTPUTS
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}/outputs/result-package",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the result package of daily merging task with given ID as a zip archive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Result package returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily task with given ID not found in the server or daily result reports not found")
    })
    public ResponseEntity<byte[]> getDailyResultPackage(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) throws IOException {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyResultPackage(taskId), "results-package.zip");
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/merging-response", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the daily Merging Response of daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging Response returned successfully"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task not found, or merging response output of daily merging task not available.")
    })
    public ResponseEntity<byte[]> getDailyMergingResponse(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyMergingResponse(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/merging-logs",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the daily merging logs of daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily merging returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task not found, or daily merging logs output of daily merging task not available.")
    })
    public ResponseEntity<byte[]> getDailyMergingLogs(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyMergingLogs(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/cgm-zip",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the daily CGM ZIP of daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily CGM ZIP returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task not found, or cgm zip output of daily merging task not available.")
    })
    public ResponseEntity<byte[]> getDailyCgmZip(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyCgmZip(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/glsk-quality-report", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the daily GLSK quality reports of daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily GLSK quality reports returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily GLSK quality reports not found")
    })
    public ResponseEntity<byte[]> getDailyGlskQualityReport(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyGlskReport(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/merging-report",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the merging report output of the daily merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging report returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily task with given ID not found in the server or merging report not found")
    })
    public ResponseEntity<byte[]> getMergingReport(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getMergingReport(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/ref-prog", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = DAILY_OUTPUTS_TAG,
            summary = "Get the daily RefProg output of daily merging task with given ID .")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Daily RefProg output returned successfully"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Daily merging task not found, or RefProg output of daily merging task not available.")
    })
    public ResponseEntity<byte[]> getDailyRefProgOutput(@Parameter(description = "Daily merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(dailyTasksManagementService.getDailyRefProgOutput(taskId));
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskManagementService;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;
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
               TASK MANAGEMENT
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    @PostMapping(value = "/tasks",
            consumes = {MULTIPART_FORM_DATA_VALUE},
            produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
            summary = "Create a new merging task and returns its data.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = CREATED, description = "Merging task created successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Invalid merging task.")})
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> createTask(@Parameter(description = "Input files ZIP archive")
                                                                      @RequestPart final MultipartFile inputFilesArchive,
                                                                      @Parameter(description = "JSON representation of the task")
                                                                      @RequestPart final String inputRequestMetadata) {
        final MergingTaskDto createdTask = taskManager.createNewTask(inputFilesArchive, inputRequestMetadata);
        final long taskId = createdTask.getId();
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
        @ApiResponse(responseCode = OK, description = "Merging task run successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID already running."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> runTask(@Parameter(description = MERGING_TASK_ID)
                                                                   @PathVariable final long taskId) {
        return ResponseEntity.ok().body(JsonApiDocument.fromData(taskManager.runTask(taskId)));
    }

    @GetMapping(value = "/tasks/{taskId}",
            produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
            summary = "Get the data of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging task data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> getTask(@Parameter(description = MERGING_TASK_ID)
                                                                   @PathVariable final long taskId) {
        return ResponseEntity.ok().body(taskManager.getTaskJsonDoc(taskId));
    }

    @GetMapping(value = "/tasks",
            produces = JSON_API_MIME_TYPE)
    @Operation(tags = TASK_MANAGEMENT_TAG,
            summary = "List existing merging tasks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging tasks list returned successfully.")
    })
    public ResponseEntity<JsonApiDocument<MergingTaskDto>> listTasks() {
        return ResponseEntity.ok().body(taskManager.getAllTasks());
    }

    @DeleteMapping(value = "/tasks/{taskId}")
    @Operation(tags = TASK_MANAGEMENT_TAG,
            summary = "Delete merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Merging task deleted successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with the given ID was not found.")
    })
    public ResponseEntity<Void> deleteTask(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        taskManager.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(value = "/tasks")
    @Operation(tags = TASK_MANAGEMENT_TAG,
            summary = "Delete all merging tasks.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Merging tasks deleted successfully.")
    })
    public ResponseEntity<Void> deleteAllTasks() {
        taskManager.deleteAllTasks();
        return ResponseEntity.noContent().build();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                    INPUTS
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
    @GetMapping(value = "/tasks/{taskId}/inputs",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get all the inputs of the merging task with given ID as a zip archive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Inputs archive returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with the given ID was not found.")
    })
    public ResponseEntity<byte[]> getInputs(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getInputsZip(taskId), "inputs_%s.zip".formatted(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/areas/{areaId}/igm", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the IGM associated to given area within merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "IGM returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Area with given ID is not part of the merging task."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getIgm(@Parameter(description = "Merging task ID") @PathVariable final long taskId, @PathVariable final String areaId) {
        return toAttachmentFileResponse(taskManager.getIgm(taskId, areaId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/areas/{areaId}/quality-report", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the IGM quality report associated to given area within merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "IGM quality report returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Area with given ID not part of the merging task."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getIgmQualityReport(@Parameter(description = "Merging task ID") @PathVariable final long taskId, @PathVariable final String areaId) {
        return toAttachmentFileResponse(taskManager.getIgmQualityReport(taskId, areaId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/generation-load-shift-keys", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the GLSK input of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "GLSK data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getGenerationLoadShiftKeys(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getGenerationLoadShiftKeys(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/external-constraints",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the external constraints input of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "External constraints data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getExternalConstraints(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getExternalConstraints(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/feasibility-ranges",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the feasibility ranges input file of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Feasibility ranges data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getFeasibilityRanges(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getFeasibilityRanges(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/dc-links", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the DC links input of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "DC links data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found.")
    })
    public ResponseEntity<byte[]> getDcLinks(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getDcLinks(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/inputs/net-position-forecast",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = INPUTS_TAG,
            summary = "Get the net positions forecast input of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Net position forecast data returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with the given ID was not found.")
    })
    public ResponseEntity<byte[]> getNetPositionForecast(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getNetPositionForecast(taskId));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
            GLOBAL CONFIGURATIONS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @PostMapping(value = "/global-configurations/virtual-hubs-configuration/publish",
            produces = JSON_API_MIME_TYPE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Publish a virtual hubs configuration with a validity interval.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Virtual hubs configuration created successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Invalid file or dates.")
    })
    public void publishVirtualHubsConfiguration(@Parameter(description = "Virtual hubs file") @RequestPart final MultipartFile configurationFile,
                                                @Parameter(description = "Valid from") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validFrom,
                                                @Parameter(description = "Valid to") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validTo) {

        taskManager.publishVirtualHubsConfiguration(configurationFile, validFrom, validTo);
    }

    @GetMapping(value = "/global-configurations/virtual-hubs-configuration",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Get valid virtual hubs configuration corresponding to the input date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Valid virtual hubs configuration  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "No valid virtual hubs configuration found for this instant.")
    })
    public ResponseEntity<byte[]> getVirtualHubsConfiguration(@Parameter(description = "Instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime instant) throws CeMergingException {
        try {
            return toAttachmentFileResponse(taskManager.getVirtualHubsConfiguration(Optional.ofNullable(instant).orElse(OffsetDateTime.now())), "virtual-hubs-config.json");
        } catch (IOException e) {
            throw new CeMergingException("Cannot generate virtual hubs configuration file", e);
        }
    }

    @PostMapping(value = "/global-configurations/xnodes-configuration/publish", produces = JSON_API_MIME_TYPE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Publish XNodes configuration with a validity interval.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "XNodes configuration created successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Invalid file or dates.")
    })
    public void publishXNodesConfiguration(@Parameter(description = "XNodes file xml") @RequestPart final MultipartFile configurationFile,
                                           @Parameter(description = "Valid from") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validFrom,
                                           @Parameter(description = "Valid to") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validTo) {

        taskManager.publishXNodesConfiguration(configurationFile, validFrom, validTo);
    }

    @GetMapping(value = "/global-configurations/xnodes-configuration", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Get valid XNodes configuration corresponding to the input date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "XNodes configuration returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "No valid XNodes configuration found for this instant.")
    })
    public ResponseEntity<byte[]> getXNodesConfiguration(@Parameter(description = "Instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime instant) throws CeMergingException {
        try {
            return toAttachmentFileResponse(taskManager.getXNodesConfiguration(Optional.ofNullable(instant).orElse(OffsetDateTime.now())), "xnodes-config.json");
        } catch (IOException e) {
            throw new CeMergingException("Cannot generate XNodes configuration file", e);
        }
    }

    @PostMapping(value = "/global-configurations/hvdc-xnode-alignment-configuration/publish", produces = JSON_API_MIME_TYPE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Publish HVDC XNode alignment configuration with a validity interval.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "HVDC XNode alignment configuration created successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Invalid file or dates.")
    })
    public void publishVirtualHubsAlignmentConfiguration(@Parameter(description = "HVDC XNode alignment json file") @RequestPart final MultipartFile configurationFile,
                                                         @Parameter(description = "Valid from") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validFrom,
                                                         @Parameter(description = "Valid to") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validTo) {

        taskManager.publishHvdcXNodeAlignmentConfiguration(configurationFile, validFrom, validTo);
    }

    @GetMapping(value = "/global-configurations/hvdc-xnode-alignment-configuration", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Get valid HVDC XNode alignment configuration corresponding to the input date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "HVDC XNode alignment configuration returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "No valid HVDC XNode alignment configuration found for this instant.")
    })
    public ResponseEntity<byte[]> getHvdcXnodeAlignementConfiguration(@Parameter(description = "Instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime instant) throws CeMergingException {
        try {
            return toAttachmentFileResponse(taskManager.getHvdcXNodeAlignmentConfiguration(Optional.ofNullable(instant).orElse(OffsetDateTime.now())), "hvdc-xnode-alignment-configuration.json");
        } catch (IOException e) {
            throw new CeMergingException("Cannot generate HVDC XNode alignment configuration file", e);
        }
    }

    @PostMapping(value = "/global-configurations/bec-configuration/publish", produces = JSON_API_MIME_TYPE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Publish BEC configuration with a validity interval.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "BEC configuration created successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Invalid file or dates.")
    })
    public void publishBECKeyConfiguration(@Parameter(description = "BEC file csv") @RequestPart final MultipartFile configurationFile,
                                           @Parameter(description = "Valid from") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validFrom,
                                           @Parameter(description = "Valid to") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validTo) {

        taskManager.publishBECKeyConfiguration(configurationFile, validFrom, validTo);
    }

    @GetMapping(value = "/global-configurations/bec-configuration", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Get valid BEC configuration corresponding to the input date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "BEC configuration  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "No valid BEC configuration found for this instant.")
    })
    public ResponseEntity<byte[]> getBecConfiguration(@Parameter(description = "Instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime instant) {
        try {
            return toAttachmentFileResponse(taskManager.getBECKeyConfiguration(Optional.ofNullable(instant).orElse(OffsetDateTime.now())), "sharingKeysBEC.json");
        } catch (IOException e) {
            throw new CeMergingException("Cannot generate BEC configuration file", e);
        }
    }

    @PostMapping(value = "/global-configurations/eic-configuration/publish", produces = JSON_API_MIME_TYPE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Publish EIC Code configuration with a validity interval.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "EIC Code configuration created successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Invalid file or dates.")
    })
    public void publishEICCodeConfiguration(@Parameter(description = "EIC Code file json") @RequestPart final MultipartFile configurationFile,
                                            @Parameter(description = "Valid from") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validFrom,
                                            @Parameter(description = "Valid to") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime validTo) {

        taskManager.publishRegionConfiguration(configurationFile, validFrom, validTo);
    }

    @GetMapping(value = "/global-configurations/eic-configuration", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = GLOBAL_CONFIGURATIONS_TAG,
            summary = "Get valid EIC Code configuration corresponding to the input date.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "EIC Code configuration returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "No valid EIC Code configuration found for this instant.")
    })
    public ResponseEntity<byte[]> getEICConfiguration(@Parameter(description = "Instant") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final OffsetDateTime instant) throws CeMergingException {
        try {
            return toAttachmentFileResponse(taskManager.getRegionConfiguration(Optional.ofNullable(instant).orElse(OffsetDateTime.now())), "region_configuration.json");
        } catch (IOException e) {
            throw new CeMergingException("Cannot generate EIC Code configuration file", e);
        }
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
              TASK CONFIGURATION
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}/configurations/dc-load-flow-parameters", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = TASK_CONFIGURATIONS_TAG,
            summary = "Get the DC load flow parameters of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The DC load flow parameters  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<byte[]> getDcLoadFlowParameters(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getDcLoadFlowParameters(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/configurations/ac-load-flow-parameters", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = TASK_CONFIGURATIONS_TAG,
            summary = "Get the AC load flow parameters of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The AC load flow parameters  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<byte[]> getAcLoadFlowParameters(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getAcLoadFlowParameters(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/configurations/basecase-improvement-parameters", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = TASK_CONFIGURATIONS_TAG,
            summary = "Get the BCI parameters of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The BCI parameters  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<byte[]> getBasecaseImprovementParameters(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getBasecaseImprovementParameters(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/configurations/balances-adjustment-parameters", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = TASK_CONFIGURATIONS_TAG,
            summary = "Get the balances adjustment parameters of the merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "The Get the balances adjustment parameters  returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<byte[]> getBalancesAdjustmenParameters(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getBalancesAdjustmentParameters(taskId));
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   ARTIFACTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}/artifacts", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get all the artifacts of the merging task with given ID as a zip archive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Atifacts archive returned successfully."),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or not reachable.")
    })
    public ResponseEntity<byte[]> getArtifacts(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getArtifactsZip(taskId), "artifacts_%s.zip".formatted(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/german-pre-merge-result",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the German pre-merged IGM artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "German pre-merged IGM returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or German pre-merged IGM not available")
    })
    public ResponseEntity<byte[]> getGermanPreMerge(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getGermanPreMerge(taskId), "germanpremerged.uct");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/dk-igm-conversion-result",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the DK converted IGM artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "DK converted IGM returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or DK converted IGM not available")
    })
    public ResponseEntity<byte[]> getDkConverted(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getDkConverted(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/topological-merge-result",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the topological merge artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Topological merge artifact returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or topological merge artifact not available")
    })
    public ResponseEntity<byte[]> getTopologicalMerge(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getTopologicalMerge(taskId), "topologicalmerged.uct");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/cgm-after-recessivity", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the cgm after recessivity artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Cgm after recessivity artifact returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or cgm after recessivity artifact not available")
    })
    public ResponseEntity<byte[]> getCgmAfterRecessivity(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getCgmAfterRecessivity(taskId), "cgm-after-recessivity.uct");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/cgm-after-pst-special-procedure",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the cgm after pst-special-procedure artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Cgm after pst-special-procedure artifact returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or cgm after pst-special-procedure artifact not available")
    })
    public ResponseEntity<byte[]> getCgmAfterPstSpecialProcedure(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getCgmAfterPstSpecialProcedure(taskId), "cgm-after-pst-special-procedure.uct");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/actual-glsk-quality-report",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the actual GLSK quality report of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Actual GLSK quality report returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or actual GLSK not available")
    })
    public ResponseEntity<byte[]> getActualGlskQualityReport(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getActualGlskReport(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/actual-glsk-correction",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the actual GLSK corrected of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Actual GLSK corrected returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or GLSK quality report not available")
    })
    public ResponseEntity<byte[]> getActualGlskCorrected(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getActualGlskCorrected(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/igms-net-positions",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the IGMs net positions artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "IGMs net positions returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or IGMs net positions not available")
    })
    public ResponseEntity<byte[]> getIgmsNetPositions(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getIgmsNetPositions(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/german-igms-net-positions", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the German IGMs net positions artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "German IGMs net positions returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or IGMs net positions not available")
    })
    public ResponseEntity<byte[]> getGermanIgmsNetPositions(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getGermanIgmsNetPositions(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/bci-output",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the BCI output artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "BCI output returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or BCI output not available")
    })
    public ResponseEntity<byte[]> getBciOutput(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getBciOutput(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/balances-adjustment-target",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the balances adjustment target artifact of the merging task with given")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Balances adjustment target returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or balances adjustment target not available")
    })
    public ResponseEntity<byte[]> getBalancesAdjustmentTarget(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getBalancesAdjustmentTarget(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/cgm-net-positions", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the CGM net positions artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "CGM net positions returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or CGM net positions not available")
    })
    public ResponseEntity<byte[]> getCgmNetPositions(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getCgmNetPositions(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/tgm-net-positions", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the TGM net positions artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "TGM net positions returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or CGM net positions not available")
    })
    public ResponseEntity<byte[]> getTgmNetPositions(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getTgmNetPositions(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/alegro-net-positions", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the alegro net positions artifact of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Alegro net positions file returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or Alegro net positions not available")
    })
    public ResponseEntity<byte[]> getAlegroNetPositions(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getAlegroNetPositions(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/balanced-cgm",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the balanced CGM of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Balanced cgm returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or CGM output of merging task not available")
    })
    public ResponseEntity<byte[]> getBalancedCgm(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getBalancedCgm(taskId), "balancedcgm.uct");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/pst-result",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the Pst process output of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Pst output returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found, or PST output of merging task not available")
    })
    public ResponseEntity<byte[]> getPstResult(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getPstOutput(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/execution-logs", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the execution logs of the merging task with given ID on e-merge format")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Execution logs returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or execution logs not available")
    })
    public ResponseEntity<byte[]> getExecutionLogs(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getExecutionLogs(taskId), "execution_logs.xml");
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/open-loadflow-logs", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the Open Loadflow logs of the merging task with given ID on the final cgm")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Open Loadflow logs  returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or Open Loadflow logs not available")
    })
    public ResponseEntity<byte[]> getOpenLoadFlowLogs(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getOpenLoadFlowLogs(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/xnodes-information",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the xnodes information file of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Xnodes information returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or xnodes information file not available")
    })
    public ResponseEntity<byte[]> getXnodesInformation(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getXnodesInformation(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/artifacts/xnodes-inconsistencies",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = ARTIFACTS_TAG,
            summary = "Get the xnodes inconsistencies file of the merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Xnodes inconsistencies returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = "Merging task with given ID has not been run"),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task with given ID not found in the server, or xnodes inconsistencies file not available")
    })
    public ResponseEntity<byte[]> getXnodesInconsistencies(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getXnodesInconsistencies(taskId));
    }

     /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    @GetMapping(value = "/tasks/{taskId}/outputs/ref-prog",
            produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
            summary = "Get the RefProg output of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "RefProg output returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or RefProg output of merging task not available")
    })
    public ResponseEntity<byte[]> getRefProgOutput(@Parameter(description = MERGING_TASK_ID)
                                                   @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getRefProg(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/cgm",
            produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
            summary = "Get the CGM output of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "CGM output returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found, or CGM output of merging task not available")
    })
    public ResponseEntity<byte[]> getCgmOutput(@Parameter(description = MERGING_TASK_ID)
                                               @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getCgm(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs",
            produces = {APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
            summary = "Get all the outputs of the merging task with given ID as a zip archive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Outputs archive returned successfully."),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found in the server, or BCI Report output of merging process task not available")
    })
    public ResponseEntity<byte[]> getOutputsByTaskId(@Parameter(description = MERGING_TASK_ID)
                                                     @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getOutputZip(taskId), "outputs_%s.zip".formatted(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/outputs/merging-logs",
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = OUTPUTS_TAG,
            summary = "Get the merging logs of merging task with given ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = OK, description = "Merging logs returned successfully"),
        @ApiResponse(responseCode = BAD_REQUEST, description = TASK_NOT_RUN),
        @ApiResponse(responseCode = NOT_FOUND, description = "Merging task not found in the server, or BCI Report output of merging process task not available")
    })
    public ResponseEntity<byte[]> getMergingLogs(@Parameter(description = "Merging task ID") @PathVariable final long taskId) {
        return toAttachmentFileResponse(taskManager.getMergingLogs(taskId));
    }

    @GetMapping(value = "/tasks/{taskId}/merging-supervisor/bci-logs", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, JSON_API_MIME_TYPE})
    @Operation(tags = MERGING_SUPERVISOR_TAG,
            summary = "Get the merging logs converted for the merging supervisor application of merging task with given ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bci logs returned successfully"),
        @ApiResponse(responseCode = "400", description = TASK_NOT_RUN),
        @ApiResponse(responseCode = "404", description = "Merging task with given ID not found in the server, or merging logs not available")
    })
    public ResponseEntity exportBciLogs(@Parameter(description = "Merging task ID") @PathVariable long taskId) {
        return toAttachmentFileResponse(taskManager.exportMergingLogs(taskId), "resultat_bci_nf.xml");
    }
}

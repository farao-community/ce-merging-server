/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.request_metadata;

import com.farao_community.farao.ce_merging.common.exception.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.Data;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.RequestMetadata;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.common.util.FileUtils.getIfInside;

public class RequestMetadataManager {
    private final String inputsPath;
    private final RequestMetadata requestMetadata;

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestMetadataManager.class);
    private static final String TASKS = "/tasks/";
    private static final String RECESSIVITY_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/default-recessivity-parameters.json";
    private static final String MISSING_FILES_ERROR = "Some input files are declared in request metadata but missing in provided archive: %s. Please ensure that files are present within archive and have the same name as request metadata.";

    private static final Map<String, Function<Inputs, SavedFile>> INPUT_GETTERS_BY_LOCATION = Map.of(
        "generation-load-shift-keys", Inputs::getGenerationLoadShiftKeys,
        "external-constraints", Inputs::getExternalConstraints,
        "feasibility-ranges", Inputs::getFeasibilityRanges,
        "net-position-forecast", Inputs::getNetPositionForecast);

    public RequestMetadataManager(final String inputsPath,
                                  final String requestJsonContent) {
        this.inputsPath = inputsPath;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            requestMetadata = mapper.readValue(requestJsonContent, RequestMetadata.class);
        } catch (final IOException e) {
            throw new ServiceIOException("Invalid request metadata", e);
        }
    }

    public RequestMetadataManager(final String inputsPath,
                                  final RequestMetadata requestMetadata) {
        this.inputsPath = inputsPath;
        this.requestMetadata = requestMetadata;
    }

    /**
     * fetches the task inputs and configuration from files specified in the request metadata
     * @param task the task to add the information to
     */
    public void feedTaskData(final MergingTask task) {
        task.setName(getRequestData().getAttributes().getName());
        task.setInputs(getTaskInputs(task.getTaskId()));
        task.setConfigurations(getTaskConfigurations(task.getTaskId()));
    }

    public ZoneOffset getParisRequestOffset() {
        return getRequestInputs()
            .getTargetDate()
            .toInstant()
            .atZone(ZoneId.of("Europe/Paris"))
            .toOffsetDateTime()
            .getOffset();
    }

    /**
     *
     * @param inputPath the path to search inputs in
     * @throws IOException if any mandatory file is not provided
     */
    public void checkIfAllInputsAvailable(final Path inputPath) throws IOException {
        final List<String> missingFiles = new ArrayList<>();

        getRequestInputs()
            .getIgms()
            .forEach(igm -> checkIfAvailable(igm, inputPath, missingFiles));

        INPUT_GETTERS_BY_LOCATION
            .values()
            .forEach(getter ->
                         checkIfAvailable(getter.apply(getRequestInputs()),
                                          inputPath,
                                          missingFiles));

        if (!missingFiles.isEmpty()) {
            FileSystemUtils.deleteRecursively(inputPath);
            final String message = String.format(MISSING_FILES_ERROR, String.join(", ", missingFiles));
            LOGGER.error(message);
            throw new TaskNotValidException(message);
        }
    }

    private Inputs getTaskInputs(final long taskId) {
        final Inputs inputs = getRequestInputs();
        final String inputsLocation = TASKS + taskId + "/inputs/";
        // update paths to make them absolute & location for GET output
        inputs.getIgms().forEach(igm -> {
            final String parentPath = inputsLocation + "areas/" + igm.getCountry();
            igm.setIgmFilePath(getInputPath(igm.getIgmFile()));
            igm.setIgmQualityReportFilePath(getInputPath(igm.getIgmQualityReportFile()));
            igm.getIgmFile()
                .setLocation(parentPath + "/igm");
            igm.getIgmQualityReportFile()
                .setLocation(parentPath + "/quality-report");
        });

        INPUT_GETTERS_BY_LOCATION
            .forEach((fileLocation, inputFileGetter) -> {
                final SavedFile inputFile = inputFileGetter.apply(inputs);
                makePathAbsolute(inputFile);
                inputFile.setLocation(inputsLocation + fileLocation);
            });
        return inputs;
    }

    private Configurations getTaskConfigurations(final long taskId) {
        final Configurations configs = getRequestData().getConfigurations();
        final String configLocation = TASKS + taskId + "/configurations/";

        Map.of("dc-load-flow-parameters", configs.getDcLoadFlowParameters(),
               "ac-load-flow-parameters", configs.getAcLoadFlowParameters(),
               "basecase-improvement-parameters", configs.getBasecaseImprovementParameters(),
               "balances-adjustment-parameters", configs.getBalancesAdjustmentParameters())
            .forEach((fileLocation, paramFile) -> {
                makePathAbsolute(paramFile);
                paramFile.setLocation(configLocation + fileLocation);
            });

        setRecessivityConfiguration(configs, configLocation);
        return configs;
    }

    private void setRecessivityConfiguration(final Configurations configurations,
                                             final String configLocation) {
        final SavedFile paramFile = configurations.getRecessivityParameters();
        paramFile.setLocation(configLocation + "recessivity-parameters");

        if (paramFile.getPath() != null) {
            paramFile.feedPathAndName(Paths.get(inputsPath, paramFile.getPath()));
        } else {
            LOGGER.info("No recessivity parameters file could be found on the input directory, Default recessivity configuration will be used");
            paramFile.feedPathAndName(Paths.get(RECESSIVITY_DEFAULT_CONFIGURATION));
        }
    }

    private void checkIfAvailable(final SavedFile savedFile,
                                  final Path parent,
                                  final List<String> missingFiles) {
        final String expectedPath = savedFile.getPath();
        if (!getIfInside(expectedPath, parent).toFile().exists()) {
            missingFiles.add(expectedPath);
        }
    }

    private void checkIfAvailable(final IgmData igmData,
                                  final Path parent,
                                  final List<String> missingFiles) {
        checkIfAvailable(igmData.getIgmFile(), parent, missingFiles);
        checkIfAvailable(igmData.getIgmQualityReportFile(), parent, missingFiles);
    }

    private Data getRequestData() {
        return requestMetadata.getData();
    }

    private Inputs getRequestInputs() {
        return getRequestData().getAttributes().getInputs();
    }

    private void makePathAbsolute(final SavedFile savedFile) {
        savedFile.feedPathAndName(getInputPath(savedFile));
    }

    private String getInputPath(final SavedFile file) {
        return getIfInside(file.getPath(),
                           Paths.get(inputsPath))
            .toString();
    }

}

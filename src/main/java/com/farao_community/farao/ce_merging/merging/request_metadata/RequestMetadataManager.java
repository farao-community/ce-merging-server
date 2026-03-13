/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.request_metadata;

import com.farao_community.farao.ce_merging.common.exception.InvalidTaskException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.RequestMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@AllArgsConstructor
@Slf4j
public class RequestMetadataManager {
    private final String inputsPath;
    private final RequestMetadata requestMetadata;

    private static final String TASKS = "/tasks/";
    private static final String RECESSIVITY_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/default-recessivity-parameters.json";
    private static final String MISSING_FILES_ERROR = "Some input files are declared in request metadata but missing in provided archive: %s. Please ensure that files are present within archive and have the same name as request metadata.";

    private static final Map<String, Function<Inputs, SavedFile>> INPUT_GETTERS_BY_LOCATION = Map.of(
        "generation-load-shift-keys", Inputs::getGenerationLoadShiftKeys,
        "external-constraints", Inputs::getExternalConstraints,
        "feasibility-ranges", Inputs::getFeasibilityRanges,
        "dc-links", Inputs::getDcLinks,
        "net-position-forecast", Inputs::getNetPositionForecast);

    public RequestMetadataManager(final String inputsPath, final String requestJsonContent) {
        this.inputsPath = inputsPath;
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            requestMetadata = mapper.readValue(requestJsonContent, RequestMetadata.class);
        } catch (final IOException e) {
            throw new ServiceIOException("Invalid request metadata", e);
        }
    }

    public ZoneOffset getRealRequestOffset() {
        return requestMetadata
            .getData()
            .getAttributes()
            .getInputs()
            .getTargetDate()
            .toInstant()
            .atZone(ZoneId.of("Europe/Paris"))
            .toOffsetDateTime()
            .getOffset();
    }

    public void checkIfAllInputsAvailable(final Path tmpInputPath) throws IOException {
        final List<String> missingFiles = new ArrayList<>();

        final Inputs inputs = requestMetadata.getData().getAttributes().getInputs();

        inputs.getIgms()
            .forEach(igm -> {
                checkIfAvailable(igm.getIgmFile(), tmpInputPath, missingFiles);
                checkIfAvailable(igm.getIgmQualityReportFile(), tmpInputPath, missingFiles);
            });

        INPUT_GETTERS_BY_LOCATION
            .values()
            .forEach(getter -> checkIfAvailable(getter.apply(inputs), tmpInputPath, missingFiles));

        if (!missingFiles.isEmpty()) {
            FileSystemUtils.deleteRecursively(tmpInputPath);
            final String message = String.format(MISSING_FILES_ERROR, String.join(" - ", missingFiles));
            log.error(message);
            throw new InvalidTaskException(message);
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

    public void feedTaskData(final MergingTask task) {
        task.setName(requestMetadata.getData().getAttributes().getName());
        setTaskInputs(task, requestMetadata.getData().getAttributes().getInputs());
        setTaskConfigurations(task, requestMetadata.getData().getConfigurations());
    }

    private void setTaskInputs(final MergingTask task,
                               final Inputs inputs) {
        final long taskId = task.getTaskId();
        final String inputsLocation = TASKS + taskId + "/inputs/";
        // update paths to make them absolute & location for GET output
        inputs.getIgms().forEach(igm -> {
            igm.setIgmFilePath(getIfInside(igm.getIgmFile().getPath(), inputPath()).toString());
            igm.setIgmQualityReportFilePath(getIfInside(igm.getIgmQualityReportFile().getPath(),
                                                        inputPath()).toString());
            igm.getIgmFile().setLocation(inputsLocation + "areas/" + igm.getCountry() + "/igm");
            igm.getIgmQualityReportFile().setLocation(inputsLocation + "areas/" + igm.getCountry() + "/quality-report");
        });

        INPUT_GETTERS_BY_LOCATION
            .forEach((fileLocation, inputFileGetter) -> {
                final SavedFile inputFile = inputFileGetter.apply(inputs);
                makePathAbsolute(inputFile);
                inputFile.setLocation(inputsLocation + fileLocation);
            });

        task.setInputs(inputs);
    }

    private void setTaskConfigurations(final MergingTask task,
                                       final Configurations configs) {
        final String configLocation = TASKS + task.getTaskId() + "/configurations/";

        Map.of("dc-load-flow-parameters", configs.getDcLoadFlowParameters(),
               "ac-load-flow-parameters", configs.getAcLoadFlowParameters(),
               "basecase-improvement-parameters", configs.getBasecaseImprovementParameters(),
               "balances-adjustment-parameters", configs.getBalancesAdjustmentParameters())
            .forEach((fileLocation, paramFile) -> {
                makePathAbsolute(paramFile);
                paramFile.setLocation(configLocation + fileLocation);
            });

        setRecessivityConfiguration(configs, configLocation);
        task.setConfigurations(configs);
    }

    private void makePathAbsolute(final SavedFile savedFile) {
        savedFile.feedPathAndName(getIfInside(savedFile.getPath(),
                                              inputPath())
                                      .toString());
    }

    private void setRecessivityConfiguration(final Configurations configurations,
                                             final String configLocation) {
        final SavedFile paramFile = configurations.getRecessivityParameters();
        paramFile.setLocation(configLocation + "recessivity-parameters");

        if (paramFile.getPath() != null) {
            paramFile.feedPathAndName(Paths.get(inputsPath, configurations.getRecessivityParameters().getPath()).toString());
        } else {
            log.info("No recessivity parameters file could be found on the input directory, Default recessivity configuration will be used");
            paramFile.feedPathAndName(Paths.get(RECESSIVITY_DEFAULT_CONFIGURATION).toString());
        }
    }

    private Path inputPath() {
        return Paths.get(inputsPath);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.metadata;

import com.farao_community.farao.ce_merging.common.exception.InvalidTaskException;
import com.farao_community.farao.ce_merging.merging.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.metadata.model.RequestMetadata;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.ZONE_OF_PARIS;

@AllArgsConstructor
@Slf4j
public class RequestMetadataManager {
    private final String inputsPath;
    private final RequestMetadata requestMetadata;

    private static final String TASKS = "/tasks/";
    private static final String RECESSIVITY_DEFAULT_CONFIGURATION = "gridDefaultConfigurations/default-recessivity-parameters.json";
    private static final String MISSING_FILES_ERROR = "Some input files are declared in request metadata but missing in provided archive: %s. Please ensure that files are present within archive and have the same name as request metadata.";

    public ZoneOffset getRealRequestOffset() {
        return requestMetadata
            .getData()
            .getAttributes()
            .getInputs()
            .getTargetDate()
            .toInstant()
            .atZone(ZONE_OF_PARIS)
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

        checkIfAvailable(inputs.getGenerationLoadShiftKeys(), tmpInputPath, missingFiles);
        checkIfAvailable(inputs.getExternalConstraints(), tmpInputPath, missingFiles);
        checkIfAvailable(inputs.getFeasibilityRanges(), tmpInputPath, missingFiles);
        checkIfAvailable(inputs.getDcLinks(), tmpInputPath, missingFiles);
        checkIfAvailable(inputs.getNetPositionForecast(), tmpInputPath, missingFiles);

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
        if (!parent.resolve(expectedPath).toFile().exists()) {
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
            igm.setIgmFilePath(Paths.get(inputsPath, igm.getIgmFile().getPath()).toString());
            igm.setIgmQualityReportFilePath(Paths.get(inputsPath, igm.getIgmQualityReportFile().getPath()).toString());
            igm.getIgmFile().setLocation(inputsLocation + "areas/" + igm.getCountry() + "/igm");
            igm.getIgmQualityReportFile().setLocation(inputsLocation + "areas/" + igm.getCountry() + "/quality-report");
        });

        Map.of("generation-load-shift-keys", inputs.getGenerationLoadShiftKeys(),
               "external-constraints", inputs.getExternalConstraints(),
               "feasibility-ranges", inputs.getFeasibilityRanges(),
               "dc-links", inputs.getDcLinks(),
               "net-position-forecast", inputs.getNetPositionForecast())
            .forEach((fileLocation, inputFile) -> {
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
               "basecase-improvement-parameters", configs.getBaseCaseImprovementParameters(),
               "balances-adjustment-parameters", configs.getBalancesAdjustmentParameters())
            .forEach((fileLocation, paramFile) -> {
                makePathAbsolute(paramFile);
                paramFile.setLocation(configLocation + fileLocation);
            });

        setRecessivityConfiguration(configs, configLocation);
        task.setConfigurations(configs);
    }

    private void makePathAbsolute(final SavedFile savedFile) {
        savedFile.feedPathAndName(Paths.get(inputsPath, savedFile.getPath()).toString());
    }

    private void setRecessivityConfiguration(final Configurations configurations,
                                             final String configLocation) {
        final SavedFile paramFile = configurations.getRecessivityParameters();
        paramFile.setLocation(configLocation + "recessivity-parameters");

        if (paramFile.getPath() != null) {
            paramFile.feedPathAndName(Paths.get(inputsPath, configurations.getRecessivityParameters().getPath()).toString());
        } else {
            try {
                log.info("No recessivity parameters file could be found on the input directory, Default recessivity configuration will be used");
                paramFile.feedPathAndName(new ClassPathResource(RECESSIVITY_DEFAULT_CONFIGURATION).getFile().getAbsolutePath());
            } catch (final IOException e) {
                log.warn("No default recessivity configuration file could be found, no country will be considered recessive");
            }
        }
    }
}

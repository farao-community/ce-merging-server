/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.export_results;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExportTaskResultsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTaskResultsService.class);
    private final CeMergingConfiguration configuration;
    private final MergingTaskRepository mergingTaskRepository;

    public ExportTaskResultsService(final MergingTaskRepository mergingTaskRepository, final CeMergingConfiguration configuration) {
        this.mergingTaskRepository = mergingTaskRepository;
        this.configuration = configuration;
    }

    public void generateOutputFiles(final MergingTask mergingTask) {
        try {
            mergingTask.getOutputs().setRealGlsk(copyFileToOutputDirectory(mergingTask, mergingTask.getArtifacts().getFile(ArtifactType.GLSK_QUALITY_REPORT), OutputType.GLSK_QUALITY_REPORT));
            copyIgmQualityReportInOutputDirectory(mergingTask);
            mergingTaskRepository.save(mergingTask);
        } catch (Exception e) {
            final String errorMessage = String.format("Results export failed for task %d with target date %s, cause: %s", mergingTask.getId(), mergingTask.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void copyIgmQualityReportInOutputDirectory(final MergingTask mergingTask) {
        final List<IgmData> igmsData = mergingTask.getInputs().getIgms();
        final Map<String, SavedFile> qualityChecksData = new HashMap<>();
        igmsData.forEach(igm -> {
            SavedFile savedFileIgm = copyFileToOutputDirectory(mergingTask, igm.getIgmQualityReportFile(), OutputType.IGM_DATA);
            qualityChecksData.put(igm.getCountry(), savedFileIgm);
        });
        mergingTask.getOutputs().setIgmQualityChecks(qualityChecksData);
    }

    private SavedFile copyFileToOutputDirectory(final MergingTask mergingTask, final SavedFile savedFile, final OutputType outputType) {
        try (InputStream inputStream = new FileInputStream(savedFile.getPath())) {
            final String fileName;
            final String location;
            if (outputType == OutputType.IGM_DATA) {
                fileName = savedFile.getOriginalName();
                location = outputType.getLocation(mergingTask.getId(), mergingTask.getTargetDate());
            } else {
                fileName = outputType.getFileName(mergingTask.getInputs().getTargetDate());
                location = outputType.getLocation(mergingTask.getId());
            }
            return FileStorageUtils.save(
                    configuration.getOutputsDirectoryPath(mergingTask),
                    fileName,
                    location,
                    path -> saveInOutput(inputStream, path)
            );
        } catch (IOException e) {
            throw new ServiceIOException(String.format("Error while copying %s file name in output folder  for task '%d' In Export results process.", savedFile.getOriginalName(), mergingTask.getId()), e);
        }
    }

    private void saveInOutput(final InputStream inputStream, final Path filePath) {
        try {
            Files.createDirectories(filePath.getParent());
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ServiceIOException(String.format("Error while writing file in path %s", filePath), e);
        }
    }
}

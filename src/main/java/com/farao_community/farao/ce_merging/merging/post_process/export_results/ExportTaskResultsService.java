/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 */
package com.farao_community.farao.ce_merging.merging.post_process.export_results;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
@Service
public class ExportTaskResultsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportTaskResultsService.class);
    private final CeMergingConfiguration configuration;
    private final MergingTaskRepository mergingTaskRepository;
    private static final String IGM_QUALITY_CHECK_DIRECTORY_NAME = "igm_quality_check";
    // TODO private final LogsCustomisationService logsCustomisationService;

    public ExportTaskResultsService(MergingTaskRepository mergingTaskRepository, CeMergingConfiguration configuration) { //TODO , LogsCustomisationService logsCustomisationService) {
        this.mergingTaskRepository = mergingTaskRepository;
        this.configuration = configuration;
        //TODO this.logsCustomisationService = logsCustomisationService;
    }

    public void generateOutPutFiles(MergingTask mergingTask) {
        // TODO logsCustomisationService.setExtraFieldsInLogsMdc(mergingTask.getTaskId(), MergingCoreStep.RESULTS_EXPORT.toString());
        try {
            String outputsDirectoryPath = configuration.getOutputsDirectoryPath(mergingTask);
            mergingTask.getOutputs().setRealGlsk(copyFileToOutputDirectory(mergingTask, mergingTask.getArtifacts().getFile(ArtifactType.GLSK_QUALITY_REPORT), outputsDirectoryPath));
            copyIgmQualityReportInOutputDirectory(mergingTask);
            mergingTaskRepository.save(mergingTask);
        } catch (Exception e) {
            String errorMessage = String.format("Results export failed for task %d with target date %s, cause: %s", mergingTask.getId(), mergingTask.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private String getIgmQualityReportFileName(OffsetDateTime dateTime) {
        String dateAndTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withLocale(Locale.FRANCE).format(dateTime);
        return String.format("/%s_%s/", dateAndTime, IGM_QUALITY_CHECK_DIRECTORY_NAME);
    }

    private void copyIgmQualityReportInOutputDirectory(MergingTask mergingTask) {
        List<IgmData> igmsData = mergingTask.getInputs().getIgms();
        Map<String, SavedFile> qualityChecksData = new HashMap<>();
        String outputDirectoryPath = String.format("%s/%s", configuration.getOutputsDirectoryPath(mergingTask), getIgmQualityReportFileName(mergingTask.getInputs().getTargetDate()));
        igmsData.forEach(igm -> {
            SavedFile savedFileIgm = copyFileToOutputDirectory(mergingTask, igm.getIgmQualityReportFile(), outputDirectoryPath);
            qualityChecksData.put(igm.getCountry(), savedFileIgm);
        });
        mergingTask.getOutputs().setIgmQualityChecks(qualityChecksData);
    }

    private SavedFile copyFileToOutputDirectory(MergingTask mergingTask, SavedFile savedFile, String outPutDirectoryPath) {
        try (InputStream inputStream = new FileInputStream(savedFile.getPath())) {
            return saveInOutput(savedFile.getOriginalName(), mergingTask, inputStream, outPutDirectoryPath);
        } catch (IOException e) {
            throw new ServiceIOException(String.format("Error while copying %s file name in output folder  for task '%d' In Export results process.", savedFile.getOriginalName(), mergingTask.getId()), e);
        }
    }

    private SavedFile saveInOutput(String fileName, MergingTask mergingTask, InputStream inputStream, String path) {
        Path filePath = Paths.get(path, fileName);
        try {
            byte[] file = IOUtils.toByteArray(inputStream);
            File files = new File(filePath.toString());
            files.getParentFile().mkdirs();
            Files.write(filePath, file);
        } catch (IOException e) {
            throw new ServiceIOException(String.format("Error while writing file in path %s", filePath.toString()), e);
        }
        return new SavedFile(fileName, filePath.toString(), String.format("/tasks/%d/outputs/%s", mergingTask.getId(), fileName.toLowerCase()));
    }
}

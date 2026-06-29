package com.farao_community.farao.ce_merging.merging.service;

import com.farao_community.farao.ce_merging.common.chain.Handler;
import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractMergingService implements Handler<MergingTask> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMergingService.class);
    protected final MergingTaskRepository tasksRepository;
    protected final CeMergingConfiguration configuration;

    protected AbstractMergingService(final MergingTaskRepository tasksRepository, final CeMergingConfiguration configuration) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
    }

    public <T> void saveFileInArtifacts(T businessObject, final MergingTask task) {
        String fileName = "forecastReferenceProgram.json";
        Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(task), fileName);
        JsonUtils.writeInPath(businessObject.getClass(), businessObject, filePath);
        SavedFile referenceProgramForecastFile = new SavedFile(fileName, filePath.toString(), String.format("/tasks/%d/artifacts/netPositionForecast", task.getTaskId()));
        task.getArtifacts().setReferenceProgramForecastFile(referenceProgramForecastFile);
        LOGGER.info("File '{}' is saved in task '{}' artifacts", fileName, task.getTaskId());
    }

}

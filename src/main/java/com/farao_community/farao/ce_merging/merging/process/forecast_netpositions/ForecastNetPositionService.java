package com.farao_community.farao.ce_merging.merging.process.forecast_netpositions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ForecastNetPositionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForecastNetPositionService.class);
    private final MergingTaskRepository repository;
    private final CeMergingConfiguration configuration;

    public ForecastNetPositionService(MergingTaskRepository repository, CeMergingConfiguration configuration) {
        this.repository = repository;
        this.configuration = configuration;
    }

    public void importForecastNetPosition(MergingTask task) {
        try {
            ReferenceProgram referenceProgram = ForecastNetPositionImporter.importFromFile(task.getInputs().getNetPositionForecast().getPath(), task.getInputs().getTargetDate());
            FileStorageUtils.saveArtifactFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE, referenceProgram, task, configuration);
            repository.save(task);
        } catch (Exception e) {
            String errorMessage = String.format("Import of net position file failed for task %d with target date %s", task.getId(), task.getInputs().getTargetDate());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }
}

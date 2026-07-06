/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskAlreadyRunningException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotFoundException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotRunException;
import com.farao_community.farao.ce_merging.common.exception.task.TaskNotValidException;
import com.farao_community.farao.ce_merging.common.json_api.JsonApiDocument;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.BECKeyConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.HvdcAlignmentConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.RegionConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.VirtualHubsConfigurationService;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.XNodeConfigurationService;
import com.farao_community.farao.ce_merging.merging.MergingService;
import com.farao_community.farao.ce_merging.merging.request_metadata.RequestMetadataManager;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.mapper.MergingTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.common.util.ZipUtils.unzipInputFileInTmp;
import static com.farao_community.farao.ce_merging.common.util.ZipUtils.zipDirectory;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.ERROR;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.RUNNING;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.SUCCESS;
import static java.nio.file.Files.createDirectories;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.springframework.util.FileSystemUtils.copyRecursively;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

@Service
public class MergingTaskManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergingTaskManagementService.class);

    private final CeMergingConfiguration configuration;
    private final MergingService mergingService;
    private final MergingTaskRepository repository;
    private final MergingTaskMapper mapper;
    private final VirtualHubsConfigurationService virtualHubsConfigurationService;
    private final XNodeConfigurationService xNodeConfigurationService;
    private final BECKeyConfigurationService becKeyConfigurationService;
    private final RegionConfigurationService regionConfigurationService;
    private final HvdcAlignmentConfigurationService hvdcAlignmentConfigurationService;

    public MergingTaskManagementService(final CeMergingConfiguration configuration,
                                        final MergingService mergingService,
                                        final MergingTaskRepository repository,
                                        final MergingTaskMapper mapper,
                                        final VirtualHubsConfigurationService virtualHubsConfigurationService,
                                        final XNodeConfigurationService xNodeConfigurationService,
                                        final BECKeyConfigurationService becKeyConfigurationService,
                                        final RegionConfigurationService regionConfigurationService,
                                        final HvdcAlignmentConfigurationService hvdcAlignmentConfigurationService) {
        this.configuration = configuration;
        this.mergingService = mergingService;
        this.repository = repository;
        this.mapper = mapper;
        this.virtualHubsConfigurationService = virtualHubsConfigurationService;
        this.xNodeConfigurationService = xNodeConfigurationService;
        this.becKeyConfigurationService = becKeyConfigurationService;
        this.regionConfigurationService = regionConfigurationService;
        this.hvdcAlignmentConfigurationService = hvdcAlignmentConfigurationService;
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        TASKS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public MergingTaskDto runTask(final Long taskId) {
        return mapper.mergingTaskToMergingTaskDto(run(getTaskById(taskId)));
    }

    public JsonApiDocument<MergingTaskDto> getTaskJsonDoc(final Long taskId) {
        return JsonApiDocument.fromData(
            mapper.mergingTaskToMergingTaskDto(
                getTaskById(taskId)
            )
        );
    }

    public MergingTaskDto createNewTask(final MultipartFile inputZip,
                                        final String inputRequestMetadata) {
        // empty at this stage, but done to init ID to be able to create directories
        final MergingTask task = repository.save(new MergingTask());

        final String inputsDir = configuration.getInputsDirectoryPath(task);
        final RequestMetadataManager requestMgr = new RequestMetadataManager(inputsDir, inputRequestMetadata);

        final Path inputsPath = Path.of(inputsDir);
        try {
            final Path tmpInputPath = unzipInputFileInTmp(inputZip);
            requestMgr.checkIfAllInputsAvailable(tmpInputPath);

            // create data in tmp folder then move it to the permanent one
            createDirectories(inputsPath);
            createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
            createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));

            copyRecursively(tmpInputPath, inputsPath);
            deleteRecursively(tmpInputPath);

            task.setArchiveFileOriginalName(inputZip.getOriginalFilename());
            requestMgr.feedTaskData(task);
            task.getInputs().setRealOffset(requestMgr.getParisRequestOffset());

            repository.save(task);
            LOGGER.info("Merging task created with id: {}", task.getId());
            return mapper.mergingTaskToMergingTaskDto(task);
        } catch (final Exception e) {
            final String error = "Error during merging task creation";
            LOGGER.error(error, e);
            repository.delete(task);
            deleteQuietly(inputsPath.toFile());
            throw new ServiceIOException(error, e);
        }
    }

    public JsonApiDocument getAllTasks() {
        final Iterable<MergingTask> tasks = repository.findAll();
        tasks.forEach(this::handleDaylightSavingTime);
        return JsonApiDocument.fromDataList(mapper.mergingTasksToMergingTasksDto(tasks));
    }

    public void deleteTask(final Long taskId) {
        try {
            MergingTask task = getTaskById(taskId);
            FileSystemUtils.deleteRecursively(Paths.get(configuration.getTaskDirectoryPath(task)));
            // Delete entity in database
            repository.deleteById(taskId);
        } catch (IOException e) {
            throw new CeMergingException("Error during merging task delete", e);
        }
    }

    public void deleteAllTasks() {
        repository.findAll().forEach(taskEntity -> deleteTask(taskEntity.getId()));
    }

        /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        INPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public byte[] getInputsZip(final Long taskId) {
        return zipDirectory(configuration.getInputsDirectoryPath(getTaskById(taskId)));
    }

    public SavedFile getIgm(final Long taskId, final String areaId) {
        return getIgmData(taskId, areaId).getIgmFile();
    }

    public SavedFile getIgmQualityReport(final Long taskId, final String areaId) {
        return getIgmData(taskId, areaId).getIgmQualityReportFile();
    }

    public SavedFile getGenerationLoadShiftKeys(final Long taskId) {
        return getInputFile(taskId, Inputs::getGenerationLoadShiftKeys);
    }

    public SavedFile getExternalConstraints(final Long taskId) {
        return getInputFile(taskId, Inputs::getExternalConstraints);
    }

    public SavedFile getFeasibilityRanges(final Long taskId) {
        return getInputFile(taskId, Inputs::getFeasibilityRanges);
    }

    public SavedFile getNetPositionForecast(final Long taskId) {
        return getInputFile(taskId, Inputs::getNetPositionForecast);
    }

    public SavedFile getDcLinks(final Long taskId) {
        return getInputFile(taskId, Inputs::getDcLinks);
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                      TASK CONFIGURATIONS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getDcLoadFlowParameters(final Long taskId) {
        return getConfigurationFile(taskId, Configurations::getDcLoadFlowParameters);
    }

    public SavedFile getAcLoadFlowParameters(final Long taskId) {
        return getConfigurationFile(taskId, Configurations::getAcLoadFlowParameters);
    }

    public SavedFile getBasecaseImprovementParameters(final Long taskId) {
        return getConfigurationFile(taskId, Configurations::getBasecaseImprovementParameters);
    }

    public SavedFile getBalancesAdjustmentParameters(final Long taskId) {
        return getConfigurationFile(taskId, Configurations::getBalancesAdjustmentParameters);
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        ARTIFACTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public byte[] getArtifactsZip(final Long taskId) {
        return zipDirectory(configuration.getArtifactsDirectoryPath(getTaskById(taskId)));
    }

    public SavedFile getXnodesInformation(final Long taskId) {
        return getArtifacts(taskId).getFile(XNODES_INFORMATION_FILE);
    }

    public SavedFile getCgmNetPositions(final Long taskId) {
        return getArtifacts(taskId).getFile(CGM_NET_POSITIONS_FILE);
    }

    public SavedFile getDkConverted(final Long taskId) {
        return getArtifacts(taskId).getFile(DK_CONVERTED_FILE);
    }

    public byte[] getGermanPreMerge(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public byte[] getTopologicalMerge(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public byte[] getCgmAfterRecessivity(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getActualGlskReport(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public byte[] getCgmAfterPstSpecialProcedure(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getActualGlskCorrected(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getIgmsNetPositions(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getGermanIgmsNetPositions(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getBciOutput(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getBalancesAdjustmentTarget(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getTgmNetPositions(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getAlegroNetPositions(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public byte[] getBalancedCgm(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getPstOutput(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public byte[] getExecutionLogs(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getOpenLoadFlowLogs(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }

    public SavedFile getXnodesInconsistencies(final Long taskId) {
        //TODO: Implement. The method signature can be changed if necessary.
        return null;
    }
    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                    GLOBAL CONFIGURATIONS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public byte[] getBECKeyConfiguration(final OffsetDateTime dateTime) throws IOException {
        return becKeyConfigurationService.getConfigAsJsonBytes(dateTime);
    }

    public void publishBECKeyConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        becKeyConfigurationService.publish(configurationFile, validFrom, validTo);
    }

    public byte[] getRegionConfiguration(final OffsetDateTime dateTime) throws IOException {
        return regionConfigurationService.getConfigAsJsonBytes(dateTime);
    }

    public void publishRegionConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        regionConfigurationService.publish(configurationFile, validFrom, validTo);
    }

    public byte[] getHvdcXNodeAlignmentConfiguration(final OffsetDateTime dateTime) throws IOException {
        return hvdcAlignmentConfigurationService.getConfigAsJsonBytes(dateTime);
    }

    public void publishHvdcXNodeAlignmentConfiguration(final MultipartFile configurationFile,
                                                       final OffsetDateTime validFrom,
                                                       final OffsetDateTime validTo) {
        hvdcAlignmentConfigurationService.publish(configurationFile, validFrom, validTo);
    }

    public byte[] getVirtualHubsConfiguration(final OffsetDateTime dateTime) throws IOException {
        return virtualHubsConfigurationService.getConfigAsJsonBytes(dateTime);
    }

    public void publishVirtualHubsConfiguration(final MultipartFile configurationFile,
                                                final OffsetDateTime validFrom,
                                                final OffsetDateTime validTo) {
        virtualHubsConfigurationService.publish(configurationFile, validFrom, validTo);
    }

    public byte[] getXNodesConfiguration(final OffsetDateTime dateTime) throws IOException {
        return xNodeConfigurationService.getConfigAsJsonBytes(dateTime);
    }

    public void publishXNodesConfiguration(final MultipartFile configurationFile,
                                           final OffsetDateTime validFrom,
                                           final OffsetDateTime validTo) {
        xNodeConfigurationService.publish(configurationFile, validFrom, validTo);
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                        OUTPUTS
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public SavedFile getCgm(final Long taskId) {
        return getOutputs(taskId).getCgm();
    }

    public SavedFile getRefProg(final Long taskId) {
        return getOutputs(taskId).getRefProg();
    }

    public byte[] getOutputZip(final Long taskId) {
        return zipDirectory(
            configuration.getOutputsDirectoryPath(
                getFinishedTaskById(taskId)
            )
        );
    }

    public SavedFile getMergingLogs(final Long taskId) {
        return getOutputs(taskId).getMergingLogs();
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   MERGING SUPERVISOR
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    public byte[] exportMergingLogs(long taskId) {
        //TODO
        return null;
    }

    /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                   INTERNAL (PRIVATE)
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

    private MergingTask run(final MergingTask task) {
        try {
            if (task.getStatus() == RUNNING) {
                throw new TaskAlreadyRunningException(String.format("Task %d already running, could not be run again",
                                                                    task.getId()));
            }
            task.setStatus(RUNNING);

            LOGGER.info("Merging task {} is running.", task.getId());
            repository.save(task);

            LOGGER.info("Running merging task {}' ", task.getId());
            mergingService.run(task);
            task.setStatus(SUCCESS);
            repository.save(task);
            LOGGER.info("Merging task {} succeeded", task.getId());
            return task;

        } catch (final TaskAlreadyRunningException alreadyRunningException) {
            throw alreadyRunningException;
        } catch (final Exception e) {
            final String error = e.getMessage();
            task.setStatusDetail(error);
            task.setStatus(ERROR);
            repository.save(task);
            throw new CeMergingException(error, e);
        }
    }

    public Artifacts getArtifacts(final Long taskId) {
        return getFinishedTaskById(taskId).getArtifacts();
    }

    private Outputs getOutputs(final Long taskId) {
        return getFinishedTaskById(taskId).getOutputs();
    }

    private MergingTask getTaskById(final Long taskId) {
        final MergingTask task = repository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(String.format("Task %d not available", taskId)));
        handleDaylightSavingTime(task);
        return task;
    }

    private MergingTask getFinishedTaskById(final Long taskId) throws TaskNotRunException {
        final MergingTask task = getTaskById(taskId);

        return switch (task.getStatus()) {
            case CREATED -> throw new TaskNotRunException(String.format("Task %d has not been run", taskId));
            case RUNNING -> throw new TaskNotRunException(String.format("Task %d currently running", taskId));
            case null -> throw new TaskNotValidException(String.format("Task %d has no status", taskId));
            case SUCCESS, ERROR -> task;
        };
    }

    /**
     * Necessary in case of DST:
     * the second 02:30 AM (Paris time) will be UTC+2, but actually should be UTC+1
     *
     * @param task for which to adjust date if applicable
     */
    private void handleDaylightSavingTime(final MergingTask task) {
        final Inputs inputs = task.getInputs();

        final OffsetDateTime taskDate = inputs.getTargetDate();
        final ZoneOffset realOffset = inputs.getRealOffset();

        // if offsets are different, we change the target date to have it at the real offset
        if (!taskDate.getOffset().equals(realOffset)) {
            inputs.setTargetDate(OffsetDateTime.of(taskDate.toLocalDateTime(), realOffset));
        }

    }

    private SavedFile getInputFile(final Long taskId, final Function<Inputs, SavedFile> accessor) {
        return accessor.apply(getTaskById(taskId).getInputs());
    }

    private SavedFile getConfigurationFile(final Long taskId, final Function<Configurations, SavedFile> accessor) {
        return accessor.apply(getTaskById(taskId).getConfigurations());
    }

    private IgmData getIgmData(final Long taskId, final String areaId) {
        final MergingTask task = getTaskById(taskId);
        return task.getInputs().getIgm(areaId);
    }
}

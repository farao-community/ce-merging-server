/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.target_net_positions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.balances_adjustment.BalancesAdjustmentTarget;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TargetNetPositionsCalculationServiceTest {
    private static final String RESOURCES_PATH = "src/test/resources/targetNetPositionCalculation/";
    private static final String IGMS_NET_POSITIONS_FILENAME = "igmsNetPositions.json";
    private static final String BCI_OUTPUT_FILENAME = "bciOutputs.json";
    private static final String FORECAST_REFERENCE_PROGRAM_FILENAME = "forecastReferenceProgram.json";
    private static final String ALEGRO_NET_POSITIONS_FILENAME = "alegroNetPositions.json";
    private static final String BALANCE_ADJUSTMENT_TARGET_FILENAME = "balancesAdjustmentTarget.json";
    private static final String RECESSIVITY_FILENAME = "20260123_0130_2D5_UX0_RECESSIVITY_APPLIED.uct";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.of(2026, 1, 23, 10, 0, 0, 0, ZoneOffset.UTC);
    @Autowired
    TargetNetPositionsCalculationService targetNetPositionsCalculationService;

    @Autowired
    CeMergingConfiguration configuration;

    @Autowired
    MergingTaskRepository repository;

    @Test
    void computeTargetNetPositions() throws IOException {
        final MergingTask taskEntity = getCoreMergingTaskEntityWithoutVirtualHubsShifting();

        targetNetPositionsCalculationService.computeTargetNetPositions(taskEntity);

        final SavedFile balancesAdjustmentTargetFile = taskEntity.getArtifacts().getFile(ArtifactType.BALANCES_ADJUSTMENT_TARGET_FILE);
        final BalancesAdjustmentTarget balancesAdjustmentTarget = JsonUtils.read(BalancesAdjustmentTarget.class, balancesAdjustmentTargetFile.getPath());
        final BalancesAdjustmentTarget balancesAdjustmentTargetExpected = JsonUtils.read(BalancesAdjustmentTarget.class, RESOURCES_PATH + BALANCE_ADJUSTMENT_TARGET_FILENAME);

        assertEquals(balancesAdjustmentTargetExpected, balancesAdjustmentTarget);
    }

    private MergingTask getCoreMergingTaskEntityWithoutVirtualHubsShifting() throws IOException {
        final MergingTask taskEntity = new MergingTask();
        taskEntity.getInputs().setTargetDate(TARGET_DATE);
        final SavedFile igmsNetPositionsFile = new SavedFile(IGMS_NET_POSITIONS_FILENAME, RESOURCES_PATH + IGMS_NET_POSITIONS_FILENAME, "mock");
        final SavedFile bciOutputFile = new SavedFile(BCI_OUTPUT_FILENAME, RESOURCES_PATH + BCI_OUTPUT_FILENAME, "mock");
        final SavedFile forecastReferenceProgram = new SavedFile(FORECAST_REFERENCE_PROGRAM_FILENAME, RESOURCES_PATH + FORECAST_REFERENCE_PROGRAM_FILENAME, "mock");
        final SavedFile alegroNetPositions = new SavedFile(ALEGRO_NET_POSITIONS_FILENAME, RESOURCES_PATH + ALEGRO_NET_POSITIONS_FILENAME, "mock");
        final SavedFile tgmFileAfterRecessivity = new SavedFile(RECESSIVITY_FILENAME, RESOURCES_PATH + RECESSIVITY_FILENAME, "mock");

        final Artifacts artifacts = new Artifacts();
        artifacts.putFile(ArtifactType.IGMS_NET_POSITIONS_FILE, igmsNetPositionsFile);
        artifacts.putFile(ArtifactType.BCI_OUTPUT_FILE, bciOutputFile);
        artifacts.putFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE, forecastReferenceProgram);
        artifacts.putFile(ArtifactType.ALEGRO_NET_POSITIONS, alegroNetPositions);
        artifacts.putFile(ArtifactType.TGM_FILE_AFTER_RECESSIVITY, tgmFileAfterRecessivity);

        taskEntity.setArtifacts(artifacts);
        TaskTestUtils.setTaskDefaultConfigurations(taskEntity);
        final MergingTask savedTask = repository.save(taskEntity);
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(savedTask)));
        return savedTask;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.taskWithIdAndStatus;

@SpringBootTest
@TestConfiguration
class CeMergingConfigurationTest {

    @Autowired
    CeMergingConfiguration cfg;

    @Test
    void shouldObtainCorrectPaths() {
        final CeMergingConfiguration copy = new CeMergingConfiguration();
        copy.setCeMergingRoot(cfg.getCeMergingRoot());
        copy.setDailyMergingRoot(cfg.getDailyMergingRoot());
        final MergingTask task = taskWithIdAndStatus(1, TaskStatus.CREATED);

        assertEquals(Path.of("/tmp/testFiles"),
                     Path.of(copy.getCeMergingRoot()));

        assertEquals(Path.of("/tmp/testFiles/daily"),
                     Path.of(copy.getDailyMergingRoot()));

        assertEquals(Path.of("/tmp/testFiles/1/artifacts"),
                     Path.of(copy.getArtifactsDirectoryPath(task)));

        copy.setCeMergingRoot("/another/root");

        assertEquals(Path.of("/another/root/1/outputs"),
                     Path.of(copy.getOutputsDirectoryPath(task)));

        assertEquals(Path.of("/another/root/1/inputs"),
                     Path.of(copy.getInputsDirectoryPath(task)));

        assertEquals(Path.of("/tmp/testFiles/daily/1/daily-outputs"),
                     Path.of(copy.getDailyOutputsDirectoryPath(task)));

        copy.setDailyMergingRoot("/a/new/path");

        assertEquals(Path.of("/a/new/path/1/daily-inputs"),
                     Path.of(copy.getDailyInputsDirectoryPath(task)));
    }
}

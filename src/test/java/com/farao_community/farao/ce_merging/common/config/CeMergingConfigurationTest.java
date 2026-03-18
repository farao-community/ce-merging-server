/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestConfiguration
class CeMergingConfigurationTest {

    @Autowired
    CeMergingConfiguration cfg;

    @Test
    void shouldObtainCorrectPaths() {
        final MergingTask task = new MergingTask();
        task.setTaskId(1);

        assertEquals("/tmp/testFiles",
                     cfg.getCeMergingRoot());

        assertEquals("/tmp/testFiles/daily",
                     cfg.getDailyMergingRoot());

        assertEquals("/tmp/testFiles/1/artifacts",
                     cfg.getArtifactsDirectoryPath(task));

        cfg.setCeMergingRoot("/another/root");

        assertEquals("/another/root/1/outputs",
                     cfg.getOutputsDirectoryPath(task));

        assertEquals("/another/root/1/inputs",
                     cfg.getInputsDirectoryPath(task));

        assertEquals("/tmp/testFiles/daily/1/daily-outputs",
                     cfg.getDailyOutputsDirectoryPath(task));

        cfg.setDailyMergingRoot("/a/new/path");

        assertEquals("/a/new/path/1/daily-inputs",
                     cfg.getDailyInputsDirectoryPath(task));
    }
}

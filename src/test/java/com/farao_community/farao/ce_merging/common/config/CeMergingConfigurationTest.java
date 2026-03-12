/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = CeMergingConfiguration.class)
@TestConfiguration
class CeMergingConfigurationTest {

    @Autowired
    CeMergingConfiguration ceMergingConfiguration;

    @Test
    void shouldObtainCorrectPaths() {
        final MergingTask task = new MergingTask();
        task.setTaskId(1);

        assertEquals("/test/ce-merging-server/filesystem/1/artifacts",
                                ceMergingConfiguration.getArtifactsDirectoryPath(task));

        assertEquals("/test/ce-merging-server/filesystem/1/outputs",
                     ceMergingConfiguration.getOutputsDirectoryPath(task));

        assertEquals("/test/ce-merging-server/filesystem/1/inputs",
                     ceMergingConfiguration.getInputsDirectoryPath(task));
    }
}

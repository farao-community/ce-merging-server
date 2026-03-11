package com.farao_community.farao.ce_merging.common.config;

import com.farao_community.farao.ce_merging.merging.entities.MergingTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
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
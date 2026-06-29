/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.assertions.CeTaskAssert;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.CREATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.anyTask;
import static test_utils.CeTestUtils.taskWithIdAndStatus;

@SpringBootTest
class O0GridConfigurationServiceTest {

    private final MergingTaskRepository repository = mock(MergingTaskRepository.class);
    private final CeMergingConfiguration configuration = mock(CeMergingConfiguration.class);

    O0GridConfigurationService service = new O0GridConfigurationService(repository, configuration);

    @Test
    void shouldSetDefaultLoadFlowParameters() {
        final MergingTask task = taskWithIdAndStatus(1L, CREATED);
        service.handle(task);
        assertThat(task.getConfigurations().getLoadFlowParameters()).isNotNull();
        verify(repository).save(anyTask());
    }

    @Test
    void shouldSaveArtifact() {
        // must be moved later in a more suitable test class
        try (final MockedStatic<JsonUtils> jsonUtils = mockStatic(JsonUtils.class)) {
            when(configuration.getArtifactsDirectoryPath(anyTask())).thenReturn("artifacts");
            final MergingTask task = taskWithIdAndStatus(1L, CREATED);

            jsonUtils.when(() -> JsonUtils.writeInPath(any(), any(), any()))
                .thenAnswer((Answer<Void>) invocation -> null);

            service.saveArtifactFile(XNODES_INFORMATION_FILE, "TEST", task);

            CeTaskAssert.assertThat(task).hasArtifact(XNODES_INFORMATION_FILE);
        }
    }
}

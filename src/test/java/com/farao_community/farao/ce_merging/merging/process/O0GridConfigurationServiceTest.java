/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.logs.LogsCustomisationService;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest
class O0GridConfigurationServiceTest {

    private final MergingTaskRepository repository = mock(MergingTaskRepository.class);
    private final CeMergingConfiguration configuration = mock(CeMergingConfiguration.class);
    private final LogsCustomisationService logsService = mock(LogsCustomisationService.class);

    O0GridConfigurationService service = new O0GridConfigurationService(repository, configuration, logsService);

    @Test
    void shouldSetDefaultLoadFlowParameters() {
        final MergingTask task = new MergingTask();
        task.setId(1L);
        final Configurations configurations = new Configurations();
        configurations.setAcLoadFlowParameters(new SavedFile());
        task.setConfigurations(configurations);
        service.handle(task);

        assertThat(task.getConfigurations().getLoadFlowParameters()).isNotNull();
    }
}

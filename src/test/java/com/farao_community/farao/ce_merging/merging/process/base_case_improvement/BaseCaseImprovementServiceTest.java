/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.BciProcessor;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseCaseImprovementServiceTest {

    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private MergingTask task;

    private BaseCaseImprovementService service;

    @BeforeEach
    void setUp() {
        service = new BaseCaseImprovementService(configuration);
    }

    @Test
    void shouldCreateBciProcessorAndRun() {
        try (final MockedConstruction<BciProcessor> mockedConstruction = org.mockito.Mockito.mockConstruction(BciProcessor.class)) {
            service.computeTargetNetPositions(task);
            final List<BciProcessor> processors = mockedConstruction.constructed();
            assertEquals(1, processors.size());
            final BciProcessor processor = processors.get(0);
            verify(processor).run();
        }
    }
}

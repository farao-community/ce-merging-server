/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.BalancesAdjustmentProcessor;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class BalancesAdjustmentServiceTest {

    @Mock
    private CeMergingConfiguration configuration;

    @Mock
    private MergingTask task;

    @Mock
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    @Mock
    private Supplier<BalanceComputationParameters> parametersSupplier;

    private BalancesAdjustmentService service;

    @BeforeEach
    void setUp() {
        service = new BalancesAdjustmentService(configuration, loadFlowRunnerSupplier, parametersSupplier);
    }

    @Test
    void shouldCreateProcessorAndRun() throws IOException {

        try (final MockedConstruction<BalancesAdjustmentProcessor> mockedConstruction = mockConstruction(BalancesAdjustmentProcessor.class)) {

            service.shiftCgm(task);
            final List<BalancesAdjustmentProcessor> processors = mockedConstruction.constructed();
            assertEquals(1, processors.size());

            BalancesAdjustmentProcessor processor = processors.get(0);

            verify(processor).run();
        }
    }

}

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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.function.Supplier;

@Service
public class BalancesAdjustmentService {

    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> runnerSupplier;
    private final Supplier<BalanceComputationParameters> parametersSupplier;

    public BalancesAdjustmentService(
            final CeMergingConfiguration configuration,
            final Supplier<LoadFlow.Runner> runnerSupplier,
            final Supplier<BalanceComputationParameters> parametersSupplier) {
        this.configuration = configuration;
        this.runnerSupplier = runnerSupplier;
        this.parametersSupplier = parametersSupplier;
    }

    public void shiftCgm(final MergingTask task) throws IOException {
        new BalancesAdjustmentProcessor(
                task,
                configuration,
                runnerSupplier,
                parametersSupplier
        ).run();
    }
}

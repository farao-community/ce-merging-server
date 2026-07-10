/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationArea;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;

import java.util.List;

import static java.lang.Double.isNaN;
import static java.lang.Math.min;

public final class BalancesPreprocessing {

    private BalancesPreprocessing() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static void adjustGeneratorsPminWithTarget(final Network network, final List<BalanceComputationArea> areas) {

        AreasManager.on(areas, network).apply(generator -> generator.setMinP(min(generator.getMinP(),
                                                                                 generator.getTargetP())));

    }

    public static void integrateCompensation(final Network network) {
        network.getGeneratorStream()
            .forEach(BalancesPreprocessing::compensate);
    }

    public static void compensate(final Generator generator) {
        final double terminalPower = generator.getTerminal().getP();
        if (!isNaN(terminalPower)) {
            generator.setTargetP(-terminalPower);
        }
    }

}

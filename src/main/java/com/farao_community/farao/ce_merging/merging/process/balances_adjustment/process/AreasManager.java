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
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class AreasManager {
    private static final Class<Generator> GENERATOR = Generator.class;
    private final List<BalanceComputationArea> areas;
    private final Network network;

    private AreasManager(final List<BalanceComputationArea> areas, final Network network) {
        this.network = network;
        this.areas = areas;
    }

    public static AreasManager on(final List<BalanceComputationArea> areas, final Network network) {
        return new AreasManager(areas, network);
    }

    public void applyToGenerators(final Consumer<Generator> operator) {
        areas.forEach(area -> getAreaGeneratorStream(area).forEach(operator));
    }

    private Stream<Generator> getAreaGeneratorStream(final BalanceComputationArea area) {
        return area.getScalable()
            .filterInjections(network)
            .stream()
            .filter(GENERATOR::isInstance)
            .map(GENERATOR::cast);
    }

}

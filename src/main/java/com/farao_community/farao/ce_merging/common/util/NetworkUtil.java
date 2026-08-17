/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Substation;

import java.util.function.Predicate;

import static java.lang.Double.isNaN;

public final class NetworkUtil {

    private NetworkUtil() {
        // utility
    }

    public static boolean hasActivePower(final Injection injection) {
        return !isNaN(injection.getTerminal().getP());
    }

    public static double zeroIfNaN(final double value) {
        return isNaN(value) ? 0 : value;
    }

    public static boolean isConnected(final Branch branch) {
        return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
    }

    public static Predicate<Injection> isInCountry(final Country country) {
        return i -> getCountry(i) == country;
    }

    public static Predicate<Branch> isConnectedTo(final String nodeId) {
        return branch -> branch.getId().contains(nodeId);
    }

    public static Country getCountry(final Injection injection) {
        return injection.getTerminal()
            .getVoltageLevel()
            .getSubstation()
            .map(Substation::getNullableCountry)
            .orElse(null);
    }

    public static Predicate<Identifiable<?>> isIdentifiedBy(final String idRegex) {
        final Pattern idPattern = Pattern.compile(idRegex);
        return identifiable -> idPattern.matcher(identifiable.getId()).matches();
    }

    public static boolean isInOutage(final Terminal terminal) {
        return terminal == null || !terminal.isConnected();
    }

    public static boolean isInOutage(final Branch<?> branch) {
        return branch == null || isInOutage(branch.getTerminal1()) || isInOutage(branch.getTerminal2());
    }
}

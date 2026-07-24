/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.powsybl.iidm.network.Injection;

import static java.lang.Double.isNaN;

public final class NetworkUtil {

    private NetworkUtil() {
        // utility
    }

    public static boolean hasActivePower(final Injection<?> injection) {
        return !isNaN(injection.getTerminal().getP());
    }

    public static double zeroIfNaN(final double value) {
        return isNaN(value) ? 0 : value;
    }
}

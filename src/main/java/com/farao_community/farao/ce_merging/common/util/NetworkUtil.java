/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Terminal;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class NetworkUtil {

    private NetworkUtil() {
        // utility
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

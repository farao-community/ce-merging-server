/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.TwoSides;

import java.util.List;

public final class BordersUtils {

    private BordersUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static boolean isVirtualHubDanglingLine(final DanglingLine danglingLine, final List<VirtualHubRecord> virtualHubList) {
        final String nodeName = danglingLine.getPairingKey().substring(0, 8);
        return virtualHubList.stream()
            .map(VirtualHubRecord::getNodeName)
            .anyMatch(nodeName::equals);
    }

    public static Country getCountrySide(final Branch branch, final TwoSides side) {
        return branch.getTerminal(side)
            .getVoltageLevel()
            .getSubstation()
            .orElseThrow(() -> new CeMergingException(
                "Could not find substation of branch '" + branch.getId() + "'."))
            .getCountry()
            .orElseThrow(() -> new CeMergingException(
                "Could not find country in side " + side + " of branch '" + branch.getId() + "'."));
    }

    public static Country getCountry(final Injection<?> injection) {
        return injection.getTerminal()
            .getVoltageLevel()
            .getSubstation()
            .map(Substation::getNullableCountry)
            .orElse(null);
    }

}

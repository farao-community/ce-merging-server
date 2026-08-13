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
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;

import java.util.List;
import java.util.function.Predicate;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;

public final class BordersUtils {

    private BordersUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static Predicate<DanglingLine> isPairedWithVirtualHub(final List<VirtualHubRecord> virtualHubList) {
        return danglingLine -> virtualHubList.stream()
                .map(VirtualHubRecord::getNodeName)
                .anyMatch(danglingLine.getPairingKey().substring(0, 8)::equals);
    }

    public static Predicate<DanglingLine> isPairedWith(final String nodeName) {
        return l -> l.getPairingKey().equals(nodeName);
    }

    public static Country getCountryOfSide(final Branch branch,
                                           final TwoSides side) {
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
        return getCountry(injection.getTerminal());
    }

    public static Country getCountry(final Branch<?> branch, final TwoSides side) {
        return getCountry(branch.getTerminal(side));
    }

    public static Country getCountry(final HvdcLine hvdcLine, final TwoSides side) {
        return getCountry(hvdcLine.getConverterStation(side).getTerminal());
    }

    public static Country getCountry(final Terminal terminal) {
        return terminal.getVoltageLevel()
                .getSubstation()
                .map(Substation::getNullableCountry)
                .orElse(null);
    }

    public static Predicate<Branch<?>> isBranchBorderOf(final Country country) {
        return line -> isCountrySameAsOneSide(country, getCountry(line, ONE), getCountry(line, TWO));
    }

    public static Predicate<HvdcLine> isHvdcLineBorderOf(final Country country) {
        return line -> isCountrySameAsOneSide(country, getCountry(line, ONE), getCountry(line, TWO));
    }

    private static boolean isCountrySameAsOneSide(final Country country, final Country country1, final Country country2) {
        return country1 == country && country2 != country || country1 != country && country2 == country;
    }

    public static Predicate<DanglingLine> isDanglingLineBorderOf(final Country country) {
        return danglingLine -> getCountry(danglingLine) == country;
    }

    public static double getBorderFlow(final Line line, final Country country) {
        final double directFlow = (getTerminalFlow(line.getTerminal1()) - getTerminalFlow(line.getTerminal2())) / 2;
        return country.equals(getCountry(line, ONE)) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final HvdcLine hvdcLine, final Country country) {
        final double side1Flow = getTerminalFlow(hvdcLine.getConverterStation1().getTerminal());
        final double side2Flow = getTerminalFlow(hvdcLine.getConverterStation2().getTerminal());
        final double directFlow = (side1Flow - side2Flow) / 2;
        return country.equals(getCountry(hvdcLine.getConverterStation1().getTerminal())) ? directFlow : -directFlow;
    }

    public static Double getTerminalFlow(final Terminal terminal) {
        return terminal.isConnected() ? zeroIfNan(terminal.getP()) : 0;
    }

    public static Country getCountryOnOtherSide(final Line line, final Country country) {
        final Country side1Country = getCountry(line, ONE);
        final Country side2Country = getCountry(line, TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static Country getCountryOnOtherSide(final HvdcLine hvdcLine, final Country country) {
        final Country side1Country = getCountry(hvdcLine, ONE);
        final Country side2Country = getCountry(hvdcLine, TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static boolean isInMainConnectedComponent(final DanglingLine danglingLine) {
        return danglingLine.getTerminal().getBusBreakerView().getConnectableBus().isInMainConnectedComponent();
    }

}

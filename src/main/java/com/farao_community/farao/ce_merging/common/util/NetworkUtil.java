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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;
import static java.lang.Double.isNaN;

public final class NetworkUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtil.class);
    private static final String ERROR_COMPONENT_NUMBER_PARAMETER = "Component number parameter is different from 0 or 1";

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

    public static Predicate<Identifiable> isIdentifiedBy(final String idRegex) {
        final Pattern idPattern = Pattern.compile(idRegex);
        return identifiable -> idPattern.matcher(identifiable.getId()).matches();
    }

    public static boolean isInOutage(final Terminal terminal) {
        return terminal == null || !terminal.isConnected();
    }

    public static boolean isInOutage(final Branch branch) {
        return branch == null || isInOutage(branch.getTerminal1()) || isInOutage(branch.getTerminal2());
    }

    public static Predicate<DanglingLine> isPairedWithVirtualHub(final List<VirtualHubRecord> virtualHubList) {
        return danglingLine -> isPairedWithVirtualHub(danglingLine, virtualHubList);
    }

    public static boolean isPairedWithVirtualHub(final DanglingLine danglingLine,
                                                 final List<VirtualHubRecord> virtualHubList) {
        return virtualHubList.stream()
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

    public static Country getCountry(final Branch branch,
                                     final TwoSides side) {
        return getCountry(branch.getTerminal(side));
    }

    public static Country getCountry(final HvdcLine hvdcLine,
                                     final TwoSides side) {
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

    private static boolean isCountrySameAsOneSide(final Country country,
                                                  final Country country1,
                                                  final Country country2) {
        return country1 == country && country2 != country || country1 != country && country2 == country;
    }

    public static Predicate<DanglingLine> isDanglingLineBorderOf(final Country country) {
        return danglingLine -> getCountry(danglingLine) == country;
    }

    public static double getBorderFlow(final Line line,
                                       final Country country) {
        final double directFlow = (getTerminalFlow(line.getTerminal1()) - getTerminalFlow(line.getTerminal2())) / 2;
        return country.equals(getCountry(line, ONE)) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final HvdcLine hvdcLine,
                                       final Country country) {
        final double side1Flow = getTerminalFlow(hvdcLine.getConverterStation1().getTerminal());
        final double side2Flow = getTerminalFlow(hvdcLine.getConverterStation2().getTerminal());
        final double directFlow = (side1Flow - side2Flow) / 2;
        return country.equals(getCountry(hvdcLine.getConverterStation1().getTerminal())) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final Line line, final String zone) {
        final double flowSide1 = getTerminalFlow(line.getTerminal1());
        final double flowSide2 = getTerminalFlow(line.getTerminal2());
        final double directFlow = (flowSide1 - flowSide2) / 2;
        return line.getTerminal(TwoSides.ONE).getVoltageLevel().getId().startsWith(zone) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final DanglingLine danglingLine, final LoadFlowParameters.ComponentMode componentModeLfParameter) {
        return switch (componentModeLfParameter) {
            case MAIN_CONNECTED -> // Loadflow computed only on main connected component
                    danglingLine.getTerminal().getBusBreakerView().getConnectableBus().isInMainConnectedComponent() ? getLeavingFlow(danglingLine) : 0.;
            case ALL_CONNECTED ->
                    getLeavingFlow(danglingLine);
            default -> {
                LOGGER.error(ERROR_COMPONENT_NUMBER_PARAMETER);
                throw new CeMergingException(ERROR_COMPONENT_NUMBER_PARAMETER);
            }
        };
    }

    public static Double getTerminalFlow(final Terminal terminal) {
        return terminal.isConnected() ? zeroIfNaN(terminal.getP()) : 0;
    }

    public static Country getCountryOnOtherSide(final Line line,
                                                final Country country) {
        final Country side1Country = getCountry(line, ONE);
        final Country side2Country = getCountry(line, TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static Country getCountryOnOtherSide(final HvdcLine hvdcLine,
                                                final Country country) {
        final Country side1Country = getCountry(hvdcLine, ONE);
        final Country side2Country = getCountry(hvdcLine, TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static boolean isInMainConnectedComponent(final DanglingLine danglingLine) {
        return danglingLine.getTerminal().getBusBreakerView().getConnectableBus().isInMainConnectedComponent();
    }

    public static double getLeavingFlow(final DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() ? zeroIfNaN(-danglingLine.getBoundary().getP()) : 0;
    }

    public static boolean isBorderOfZone(final DanglingLine danglingLine, final String zone) {
        return isBorderOfZone(danglingLine.getId(), zone);
    }

    public static boolean isBorderOfZone(final Line line, final String zone) {
        return isBorderOfZone(line.getId(), zone);
    }

    private static boolean isBorderOfZone(final String lineId, final String zone) {
        final String nodeFrom = lineId.substring(0, 8);
        final String nodeTo = lineId.substring(9, 17);
        return nodeFrom.startsWith(zone) != nodeTo.startsWith(zone);
    }
}

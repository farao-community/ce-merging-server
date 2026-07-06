/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public final class BordersUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(BordersUtils.class);
    private static final String ERROR_COMPONENT_NUMBER_PARAMETER = "Error in net position calculation: component number parameter is different from 0 or 1";

    private BordersUtils() {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static boolean isVirtualHubDanglingLine(final DanglingLine danglingLine, final List<VirtualHubRecord> virtualHubList) {
        final List<String> virtualHubsNodeNames = virtualHubList.stream().map(VirtualHubRecord::getNodeName).collect(Collectors.toList());
        return virtualHubsNodeNames.contains(danglingLine.getPairingKey().substring(0, 8));
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

    private static double getLeavingFlow(final DanglingLine danglingLine) {
        // Should correspond to CountryArea.getLeavingFlow(danglingLine)
        return danglingLine.getTerminal().isConnected() ? zeroIfNan(-danglingLine.getBoundary().getP()) : 0;
    }

    public static double zeroIfNan(final double value) {
        return Double.isNaN(value) ? 0 : value;
    }

    public static double getBorderFlow(final Line line, final Country country) {
        // Should correspond to CountryArea.getLeavingFlow(line)
        final double directFlow = getDirectFlow(line);
        return country.equals(getCountrySide(line, TwoSides.ONE)) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final Line line, final String zone) {
        final double directFlow = getDirectFlow(line);
        return line.getTerminal(TwoSides.ONE).getVoltageLevel().getId().startsWith(zone) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(final HvdcLine hvdcLine, final Country country) {
        // Should correspond to CountryArea.getLeavingFlow(hvdcLine)
        final double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation1().getTerminal().getP()) : 0;
        final double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation2().getTerminal().getP()) : 0;
        final double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }

    public static boolean isBorderOfCountry(final DanglingLine danglingLine, final Country country) {
        Country sideCountry = getCountry(danglingLine);
        return sideCountry == country;
    }

    public static boolean isBorderOfCountry(final Branch branch, final Country country) {
        final Country side1Country = getCountrySide(branch, TwoSides.ONE);
        final Country side2Country = getCountrySide(branch, TwoSides.TWO);
        return side1Country == country && side2Country != country ||
                side1Country != country && side2Country == country;
    }

    public static boolean isBorderOfCountry(final HvdcLine hvdcLine, final Country country) {
        final Country side1Country = getCountrySide(hvdcLine, TwoSides.ONE);
        final Country side2Country = getCountrySide(hvdcLine, TwoSides.TWO);
        return side1Country == country && side2Country != country ||
                side1Country != country && side2Country == country;
    }

    public static boolean isBorderOfZone(final Identifiable<?> identifiable, final String zone) {
        return isBorderOfZone(identifiable.getId(), zone);
    }

    private static boolean isBorderOfZone(final String lineId, final String zone) {
        final String nodeFrom = lineId.substring(0, 8);
        final String nodeTo = lineId.substring(9, 17);
        return nodeFrom.startsWith(zone) != nodeTo.startsWith(zone);
    }

    public static Country getCountrySide(final Branch branch, final TwoSides side) {
        return getCountry(branch.getTerminal(side), "branch", branch.getId(), side);
    }

    public static Country getCountrySide(final HvdcLine hvdcLine, final TwoSides side) {
        return getCountry(hvdcLine.getConverterStation(side).getTerminal(), "hvdc", hvdcLine.getId(), side);
    }

    public static Country getCountry(final Injection<?> injection) {
        return injection.getTerminal().getVoltageLevel().getSubstation().isPresent() ? injection.getTerminal().getVoltageLevel().getSubstation().get().getNullableCountry() : null;
    }

    public static Country getCountryOnBorder(Line line, Country country) {
        final Country side1Country = BordersUtils.getCountrySide(line, TwoSides.ONE);
        final Country side2Country = BordersUtils.getCountrySide(line, TwoSides.TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static Country getCountryOnBorder(HvdcLine hvdcLine, Country country) {
        Country side1Country = BordersUtils.getCountrySide(hvdcLine, TwoSides.ONE);
        Country side2Country = BordersUtils.getCountrySide(hvdcLine, TwoSides.TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    private static double getDirectFlow(final Line line) {
        final double flowSide1 = line.getTerminal1().isConnected() ? zeroIfNan(line.getTerminal1().getP()) : 0;
        final double flowSide2 = line.getTerminal2().isConnected() ? zeroIfNan(line.getTerminal2().getP()) : 0;
        return (flowSide1 - flowSide2) / 2;
    }

    private static Country getCountry(final Terminal terminal, final String equipmentType, final String equipmentId, final TwoSides side) {
        return terminal.getVoltageLevel().getSubstation()
                .orElseThrow(() -> new CeMergingException("Could not find substation of " + equipmentType + " '" + equipmentId + "'."))
                .getCountry()
                .orElseThrow(() -> new CeMergingException("Could not find country in side " + side + " of " + equipmentType + " '" + equipmentId + "'."));
    }
}

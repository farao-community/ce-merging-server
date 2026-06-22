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
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Generator;
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

    public static boolean isVirtualHubDanglingLine(DanglingLine danglingLine, List<VirtualHubRecord> virtualHubList) {
        List<String> virtualHubsNodeNames = virtualHubList.stream().map(VirtualHubRecord::getNodeName).collect(Collectors.toList());
        return virtualHubsNodeNames.contains(danglingLine.getPairingKey().substring(0, 8));
    }

    public static double getBorderFlow(DanglingLine danglingLine, LoadFlowParameters.ComponentMode componentModeLfParameter) {
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

    private static double getLeavingFlow(DanglingLine danglingLine) {
        // Should correspond to CountryArea.getLeavingFlow(danglingLine)
        return danglingLine.getTerminal().isConnected() ? zeroIfNan(-danglingLine.getBoundary().getP()) : 0;
    }

    private static double zeroIfNan(double value) {
        return Double.isNaN(value) ? 0 : value;
    }

    public static double getBorderFlow(Line line, Country country) {
        // Should correspond to CountryArea.getLeavingFlow(line)
        double flowSide1 = line.getTerminal1().isConnected() ? zeroIfNan(line.getTerminal1().getP()) : 0;
        double flowSide2 = line.getTerminal2().isConnected() ? zeroIfNan(line.getTerminal2().getP()) : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(getCountrySide(line, TwoSides.ONE)) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(Line line, String zone) {
        double flowSide1 = line.getTerminal1().isConnected() ? zeroIfNan(line.getTerminal1().getP()) : 0;
        double flowSide2 = line.getTerminal2().isConnected() ? zeroIfNan(line.getTerminal2().getP()) : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return line.getTerminal(TwoSides.ONE).getVoltageLevel().getId().startsWith(zone) ? directFlow : -directFlow;
    }

    public static double getBorderFlow(HvdcLine hvdcLine, Country country) {
        // Should correspond to CountryArea.getLeavingFlow(hvdcLine)
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation1().getTerminal().getP()) : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation2().getTerminal().getP()) : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }

    public static boolean isBorderOfCountry(DanglingLine danglingLine, Country country) {
        Country sideCountry = getCountry(danglingLine);
        return sideCountry == country;
    }

    public static boolean isBorderOfCountry(Branch branch, Country country) {
        Country side1Country = getCountrySide(branch, TwoSides.ONE);
        Country side2Country = getCountrySide(branch, TwoSides.TWO);
        return side1Country == country && side2Country != country ||
                side1Country != country && side2Country == country;
    }

    public static boolean isBorderOfCountry(HvdcLine hvdcLine, Country country) {
        Country side1Country = getCountrySide(hvdcLine, TwoSides.ONE);
        Country side2Country = getCountrySide(hvdcLine, TwoSides.TWO);
        return side1Country == country && side2Country != country ||
                side1Country != country && side2Country == country;
    }

    public static boolean isBorderOfZone(DanglingLine danglingLine, String zone) {
        return isBorderOfZone(danglingLine.getId(), zone);
    }

    public static boolean isBorderOfZone(Line line, String zone) {
        return isBorderOfZone(line.getId(), zone);
    }

    private static boolean isBorderOfZone(String lineId, String zone) {
        String nodeFrom = lineId.substring(0, 8);
        String nodeTo = lineId.substring(9, 17);
        return nodeFrom.startsWith(zone) && !nodeTo.startsWith(zone) || !nodeFrom.startsWith(zone) && nodeTo.startsWith(zone);
    }

    public static Country getCountrySide(Branch branch, TwoSides side) {
        return branch.getTerminal(side).getVoltageLevel().getSubstation()
                .orElseThrow(() -> new CeMergingException(String.format("Could not find substation of branch : '%s' .", branch.getId()))).getCountry()
                .orElseThrow(() -> new CeMergingException(String.format("Could not find country in side %s of branch : '%s' .", side, branch.getId())));

    }

    public static Country getCountrySide(HvdcLine hvdcLine, TwoSides side) {
        return hvdcLine.getConverterStation(side).getTerminal().getVoltageLevel().getSubstation()
                .orElseThrow(() -> new CeMergingException(String.format("Could not find substation of hvdc : '%s' .", hvdcLine.getId()))).getCountry()
                .orElseThrow(() -> new CeMergingException(String.format("Could not find country in side %s of hvdc : '%s' .", side, hvdcLine.getId())));
    }

    public static Country getCountry(DanglingLine danglingLine) {
        return danglingLine.getTerminal().getVoltageLevel().getSubstation().isPresent() ? danglingLine.getTerminal().getVoltageLevel().getSubstation().get().getNullableCountry() : null;
    }

    public static Country getCountry(Generator generator) {
        return generator.getTerminal().getVoltageLevel().getSubstation().isPresent() ? generator.getTerminal().getVoltageLevel().getSubstation().get().getNullableCountry() : null;
    }

    public static Country getCountry(Load load) {
        return load.getTerminal().getVoltageLevel().getSubstation().isPresent() ? load.getTerminal().getVoltageLevel().getSubstation().get().getNullableCountry() : null;
    }

    public static Country getCountryOnBorder(Line line, Country country) {
        Country side1Country = BordersUtils.getCountrySide(line, TwoSides.ONE);
        Country side2Country = BordersUtils.getCountrySide(line, TwoSides.TWO);
        return country == side1Country ? side2Country : side1Country;
    }

    public static Country getCountryOnBorder(HvdcLine hvdcLine, Country country) {
        Country side1Country = BordersUtils.getCountrySide(hvdcLine, TwoSides.ONE);
        Country side2Country = BordersUtils.getCountrySide(hvdcLine, TwoSides.TWO);
        return country == side1Country ? side2Country : side1Country;
    }
}

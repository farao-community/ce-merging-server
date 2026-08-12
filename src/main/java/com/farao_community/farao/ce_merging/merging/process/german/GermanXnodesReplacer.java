/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.german;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.CurrentLimitsAdder;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.BordersUtils.getCountryOfSide;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.TopologyKind.BUS_BREAKER;
import static com.powsybl.iidm.network.TwoSides.ONE;
import static com.powsybl.iidm.network.TwoSides.TWO;

public final class GermanXnodesReplacer {
    private static final String ELEMENT_NAME_PROPERTY = "elementName";
    private static final String ELEMENT_NAME_PREFIX = "elementName_";
    private static final String TIE_LINE_LIMITS = "tieLineLimits_";
    private static final int SUBSTATION_ID_LENGTH = 6;
    private static final int VOLTAGE_LEVEL_ID_LENGTH = 7;
    private static final int BUS_ID_LENGTH = 8;
    private final Network network;

    private GermanXnodesReplacer(final Network network) {
        this.network = network;
    }

    public static void replaceXnodesWithLines(final Network network) {
        new GermanXnodesReplacer(network).replace();
    }

    private static boolean isGermanInternal(final TieLine tieLine) {
        return getCountryOfSide(tieLine, ONE) == DE && getCountryOfSide(tieLine, TWO) == DE;
    }

    void replace() {
        final List<TieLine> tieLinesToRemove = network.getTieLineStream()
                .filter(GermanXnodesReplacer::isGermanInternal)
                .toList();

        final List<DanglingLine> danglingLinesToRemove = tieLinesToRemove.stream()
                .flatMap(tieLine -> Stream.of(tieLine.getDanglingLine1(), tieLine.getDanglingLine2()))
                .toList();

        for (final TieLine tieLine : tieLinesToRemove) {
            createCorrespondingLines(tieLine);
            tieLine.remove();
        }
        danglingLinesToRemove.forEach(DanglingLine::remove);
    }

    private void createCorrespondingLines(final TieLine tieLine) {
        final String substationId = toGermanElementId(tieLine, SUBSTATION_ID_LENGTH);
        final String voltageLevelId = toGermanElementId(tieLine, VOLTAGE_LEVEL_ID_LENGTH);
        final Substation xNodeSubstation = Optional.ofNullable(network.getSubstation(substationId))
                .orElse(getDefaultSubstation(substationId));

        final VoltageLevel xNodeVoltageLevel = Optional.ofNullable(network.getVoltageLevel(voltageLevelId))
                .orElse(getDefaultVoltageLevel(tieLine, xNodeSubstation));

        final Bus xNodeBus = xNodeVoltageLevel.getBusBreakerView().newBus()
                .setId(toGermanElementId(tieLine, BUS_ID_LENGTH))
                .add();

        addTieLineSideAsNewLine(ONE, tieLine, xNodeBus, xNodeVoltageLevel);
        addTieLineSideAsNewLine(TWO, tieLine, xNodeBus, xNodeVoltageLevel);

    }

    private void addTieLineSideAsNewLine(final TwoSides side,
                                         final TieLine tieLine,
                                         final Bus xNodeBus,
                                         final VoltageLevel xNodeVoltageLevel) {
        final String sideNb = String.valueOf(side.getNum());
        final DanglingLine danglingLine = tieLine.getDanglingLine(side);
        final Terminal terminal = tieLine.getTerminal(side);
        final String bus2Id = xNodeBus.getId();
        final boolean isSide1 = side == ONE;
        final double g = danglingLine.getG();
        final double b = danglingLine.getB();

        final Line line = network.newLine()
                .setEnsureIdUnicity(true)
                .setId(String.format("%s %s 1", getBusId(terminal), xNodeBus))
                .setVoltageLevel1(terminal.getVoltageLevel().getId())
                .setVoltageLevel2(xNodeVoltageLevel.getId())
                .setBus1(getConnectedBusIdOrElse(terminal, null))
                .setBus2(bus2Id)
                .setConnectableBus1(getConnectableBusId(terminal))
                .setConnectableBus2(bus2Id)
                .setR(danglingLine.getR())
                .setX(danglingLine.getX())
                .setFictitious(danglingLine.isFictitious())
                .setG1(isSide1 ? g : 0)
                .setG2(isSide1 ? 0 : g)
                .setB1(isSide1 ? b : 0)
                .setB2(isSide1 ? 0 : b)
                .add();

        tieLine.getCurrentLimits(side).map(LoadingLimits::getPermanentLimit)
                .map(line.newOperationalLimitsGroup1(TIE_LINE_LIMITS + sideNb).newCurrentLimits()::setPermanentLimit)
                .ifPresent(CurrentLimitsAdder::add);

        Optional.ofNullable(tieLine.getProperty(ELEMENT_NAME_PREFIX + sideNb))
                .ifPresent(name -> line.setProperty(ELEMENT_NAME_PROPERTY, name));
    }

    private String getBusId(final Terminal terminal) {
        return getConnectedBusIdOrElse(terminal, getConnectableBusId(terminal));
    }

    private String getConnectedBusIdOrElse(final Terminal terminal,
                                           final String defaultValue) {
        return terminal.isConnected() ?
                terminal.getBusBreakerView().getBus().getId() :
                defaultValue;
    }

    private String getConnectableBusId(final Terminal terminal) {
        return terminal.getBusBreakerView().getConnectableBus().getId();
    }

    private Substation getDefaultSubstation(final String substationId) {
        return network.newSubstation()
                .setId(substationId)
                .setCountry(DE)
                .add();
    }

    private VoltageLevel getDefaultVoltageLevel(final TieLine tieLine,
                                                final Substation substation) {
        return substation.newVoltageLevel()
                .setId(toGermanElementId(tieLine, VOLTAGE_LEVEL_ID_LENGTH))
                .setNominalV(tieLine.getTerminal1().getVoltageLevel().getNominalV())
                .setTopologyKind(BUS_BREAKER)
                .add();
    }

    private String toGermanElementId(final TieLine tieLine,
                                     final int endIndex) {
        return "D" + tieLine.getPairingKey().substring(1, endIndex);
    }
}

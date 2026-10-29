/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.BoundaryLineAdder;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.CurrentLimitsAdder;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.ucte.network.UcteCountryCode;

import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.XNODES_PREFIX;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getNodeName;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.toCountryElementId;
import static com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa.PisaService.willBecomeFrenchXnode;
import static com.powsybl.iidm.network.Country.IT;
import static com.powsybl.iidm.network.TopologyKind.BUS_BREAKER;

public final class PisaXnodesReplacer {
    private static final String ELEMENT_NAME_PROPERTY = "elementName";
    private static final String LINE_ID_FORMAT = "%s %s 1";
    private static final int SUBSTATION_ID_LENGTH = 6;
    private static final int VOLTAGE_LEVEL_ID_LENGTH = 7;
    private static final int BUS_ID_LENGTH = 8;
    private final Network network;

    private PisaXnodesReplacer(final Network network) {
        this.network = network;
    }

    public static PisaXnodesReplacer on(final Network network) {
        return new PisaXnodesReplacer(network);
    }

    void replace(final List<BoundaryLine> boundaryLines) {

        boundaryLines.stream()
                .map(BoundaryLine::getTieLine)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(TieLine::remove);

        boundaryLines.forEach(this::createCorrespondingLine);
    }

    private void createCorrespondingLine(final BoundaryLine boundaryLine) {

        final String substationId = toItalianElementId(boundaryLine, SUBSTATION_ID_LENGTH);
        final Substation substation = network.getSubstation(substationId);
        final VoltageLevel voltageLevel = network.getVoltageLevel(toItalianElementId(boundaryLine, VOLTAGE_LEVEL_ID_LENGTH));
        final String busId = toItalianElementId(boundaryLine, BUS_ID_LENGTH);
        final Bus bus = network.getBusBreakerView().getBus(busId);
        final Substation xNodeSubstation = substation != null ? substation : getDefaultSubstation(substationId);
        final VoltageLevel xNodeVoltageLevel = voltageLevel != null ? voltageLevel : getDefaultVoltageLevel(boundaryLine, xNodeSubstation);

        final Bus xNodeBus = bus != null ? bus : xNodeVoltageLevel.getBusBreakerView().newBus()
                .setId(busId)
                .add();

        if (willBecomeFrenchXnode(boundaryLine.getTerminal())) {
            invertBoundaryLine(boundaryLine, xNodeVoltageLevel, xNodeBus);
        } else {
            createLineFromBoundaryLine(boundaryLine, xNodeBus, xNodeVoltageLevel);
        }

        boundaryLine.remove();
    }

    private void invertBoundaryLine(final BoundaryLine boundaryLine,
                                            final VoltageLevel voltageLevel,
                                            final Bus xNodeBus) {
        final Terminal terminal = boundaryLine.getTerminal();
        final boolean fromXNode = boundaryLine.getId().startsWith(XNODES_PREFIX);

        final String boundaryLineId = fromXNode ?  String.format(LINE_ID_FORMAT, xNodeBus, getBusId(terminal))
                : String.format(LINE_ID_FORMAT, getBusId(terminal), xNodeBus);

        final BoundaryLineAdder adder = voltageLevel.newBoundaryLine()
                .setB(boundaryLine.getB())
                .setG(boundaryLine.getG())
                .setR(boundaryLine.getR())
                .setX(boundaryLine.getX())
                .setPairingKey(toCountryElementId(UcteCountryCode.FR, getNodeName(terminal), BUS_ID_LENGTH))
                .setEnsureIdUnicity(true)
                .setFictitious(false)
                .setId(boundaryLineId)
                .setBus(xNodeBus.getId())
                .setP0(boundaryLine.getP0())
                .setQ0(boundaryLine.getQ0())
                .setConnectableBus(xNodeBus.getId())
                .newGeneration()// will be fixed in future release : https://github.com/powsybl/powsybl-core/pull/4105
                .setMinP(0.0)
                .setMaxP(0.0)
                .setTargetP(0.0)
                .setTargetQ(0.0)
                .setTargetV(1.0)
                .setVoltageRegulationOn(false)
                .add();

        adder.add();

    }

    private void createLineFromBoundaryLine(final BoundaryLine boundaryLine,
                                            final Bus xNodeBus,
                                            final VoltageLevel xNodeVoltageLevel) {
        final Terminal terminal = boundaryLine.getTerminal();
        final String xNodeId = xNodeBus.getId();
        final boolean fromXNode = boundaryLine.getId().startsWith(XNODES_PREFIX);
        final double g = boundaryLine.getG();
        final double b = boundaryLine.getB();

        final String lineId = fromXNode ? String.format(LINE_ID_FORMAT, xNodeBus, getBusId(terminal))
                : String.format(LINE_ID_FORMAT, getBusId(terminal), xNodeBus);

        final Line line = network.newLine()
                .setEnsureIdUnicity(true)
                .setId(lineId)
                .setVoltageLevel1(fromXNode ? xNodeVoltageLevel.getId() : terminal.getVoltageLevel().getId())
                .setVoltageLevel2(fromXNode ? terminal.getVoltageLevel().getId() : xNodeVoltageLevel.getId())
                .setBus1(fromXNode ? xNodeId : getConnectedBusIdOrElse(terminal, null))
                .setBus2(fromXNode ? getConnectedBusIdOrElse(terminal, null) : xNodeId)
                .setConnectableBus1(fromXNode ? xNodeId : getConnectableBusId(terminal))
                .setConnectableBus2(fromXNode ? getConnectableBusId(terminal) : xNodeId)
                .setR(boundaryLine.getR())
                .setX(boundaryLine.getX())
                .setFictitious(boundaryLine.isFictitious())
                .setG1(fromXNode ? g : 0)
                .setG2(fromXNode ? 0 : g)
                .setB1(fromXNode ? b : 0)
                .setB2(fromXNode ? 0 : b)
                .add();

        final CurrentLimitsAdder currentLimitsAdder = (
                fromXNode ? line.getOrCreateSelectedOperationalLimitsGroup1() : line.getOrCreateSelectedOperationalLimitsGroup2()
        ).newCurrentLimits();

        boundaryLine.getCurrentLimits()
                .map(LoadingLimits::getPermanentLimit)
                .map(currentLimitsAdder::setPermanentLimit)
                .ifPresent(CurrentLimitsAdder::add);

        Optional.ofNullable(boundaryLine.getProperty(ELEMENT_NAME_PROPERTY))
                .ifPresent(name -> line.setProperty(ELEMENT_NAME_PROPERTY, name));

    }

    public String getLineName(final String fromId, final String toId) {
        return String.format(LINE_ID_FORMAT, fromId, toId);
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
                .setCountry(IT)
                .add();
    }

    private VoltageLevel getDefaultVoltageLevel(final BoundaryLine boundaryLine,
                                                final Substation substation) {
        return substation.newVoltageLevel()
                .setId(toItalianElementId(boundaryLine, VOLTAGE_LEVEL_ID_LENGTH))
                .setNominalV(boundaryLine.getTerminal().getVoltageLevel().getNominalV())
                .setTopologyKind(BUS_BREAKER)
                .add();
    }

    private String toItalianElementId(final BoundaryLine boundaryLine,
                                      final int endIndex) {
        return toCountryElementId(UcteCountryCode.IT, boundaryLine.getPairingKey(), endIndex);
    }

}

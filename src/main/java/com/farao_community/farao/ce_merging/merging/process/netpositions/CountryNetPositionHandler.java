/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.netpositions;

import com.farao_community.farao.ce_merging.common.model.netpositions.GenerationAndLoadQuantity;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsValues;
import com.farao_community.farao.ce_merging.common.util.BordersUtils;
import com.farao_community.farao.ce_merging.common.util.CountryUtils;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.getBorderFlow;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.getCountryOnBorder;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isBranchBorderOf;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isDanglingLineBorderOf;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isHvdcLineBorderOf;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isInCountry;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isVirtualHubDanglingLine;
import static com.farao_community.farao.ce_merging.common.util.BordersUtils.zeroIfNan;
import static com.farao_community.farao.ce_merging.common.util.CountryUtils.getCountry;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getComponentMode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;
import static java.util.stream.Collectors.toSet;

public class CountryNetPositionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryNetPositionHandler.class);

    private double globalPosition;
    private double globalPositionNoVh;
    private double regionPosition;
    private double regionPositionNoVh;
    private double outBciNetPosition;
    private final Map<String, Double> globalDetailedExchanges;
    private final Map<String, Double> virtualHubsExchanges;

    private final RegionConfiguration regionConfiguration;
    private final Network network;
    private final Country country;
    private final List<VirtualHubRecord> virtualHubList;
    private final List<XnodeConfig> xnodeList;
    private final LoadFlowParameters.ComponentMode componentMode;
    private final List<Country> bciCountries;

    public static CountryNetPositionHandler buildFrom(final Country country,
                                                      final Network network,
                                                      final Configurations configurations) {
        return new CountryNetPositionHandler(configurations.getRegionConfiguration(),
                                             network,
                                             country,
                                             configurations.getVirtualHubList(),
                                             configurations.getXnodeList(),
                                             getComponentMode(configurations.getLoadFlowParameters()));
    }

    public CountryNetPositionHandler(final RegionConfiguration regionConfiguration,
                                     final Network network,
                                     final Country country,
                                     final List<VirtualHubRecord> virtualHubList,
                                     final List<XnodeConfig> xnodeList,
                                     final LoadFlowParameters.ComponentMode componentMode) {
        this.globalPosition = 0.;
        this.globalPositionNoVh = 0.;
        this.regionPosition = 0.;
        this.regionPositionNoVh = 0.;
        this.outBciNetPosition = 0.;
        this.virtualHubsExchanges = new TreeMap<>();
        this.globalDetailedExchanges = new TreeMap<>();
        this.regionConfiguration = regionConfiguration;
        this.network = network;
        this.country = country;
        this.virtualHubList = virtualHubList;
        this.xnodeList = xnodeList;
        this.componentMode = componentMode;
        this.bciCountries = regionConfiguration.getAreasAll()
                .keySet()
                .stream()
                .map(Country::valueOf)
                .toList();
    }

    public NetPositions computeNetPositions() {
        fillNetPositionsFromDanglingLines();
        fillNetPositionsFromLines();
        fillNetPositionsFromVirtualHubNodes();
        fillNetPositionsFromHvdcLines();

        return new NetPositions(new NetPositionsValues(globalPosition, globalPositionNoVh),
                                new NetPositionsValues(regionPosition, regionPositionNoVh),
                                outBciNetPosition,
                                virtualHubsExchanges,
                                globalDetailedExchanges,
                                new GenerationAndLoadQuantity(-sumOverCountry(network.getGeneratorStream()),
                                                              sumOverCountry(network.getLoadStream())));
    }

    private double sumOverCountry(final Stream<? extends Injection<?>> injectionStream) {
        return injectionStream
                .filter(isInCountry(country).and(isConnected(componentMode)))
                .map(Injection::getTerminal)
                .map(Terminal::getP)
                .mapToDouble(BordersUtils::zeroIfNan)
                .sum();
    }

    /**
     * Applies to nodes present in virtual hubs configuration but not starting by X (such as D2HWKR1D)
     * This flow is not taken into account in NetworkArea.getNetposition,
     * so it will be retrieved from targetNetpositionCalculationService
     */
    private void fillNetPositionsFromVirtualHubNodes() {
        network.getSubstationStream()
                .map(this::getVirtualHubBus)
                .filter(Objects::nonNull)
                .forEach(this::handleVirtualHubBus);
    }

    private void fillNetPositionsFromHvdcLines() {
        network.getHvdcLineStream()
                .filter(isHvdcLineBorderOf(country))
                .forEach(this::handleHvdcLine);
    }

    private void fillNetPositionsFromLines() {
        network.getLineStream()
                .filter(isBranchBorderOf(country))
                .forEach(this::handleLine);
    }

    private void fillNetPositionsFromDanglingLines() {
        network.getDanglingLineStream()
                .filter(isDanglingLineBorderOf(country))
                .forEach(this::handleDanglingLine);
    }

    private void handleVirtualHubBus(final Bus bus) {
        final double flow = zeroIfNan(bus.getP());
        globalPosition += flow;
        outBciNetPosition += flow;

        addToVirtualHubExchange(bus.getId(), flow);

        if (isCountryInRegionConfig()) {
            regionPosition += flow;
        }
    }

    private void handleHvdcLine(final HvdcLine hvdcLine) {
        double borderFlow = getBorderFlow(hvdcLine, country);
        updatePositions(borderFlow, getCountryOnBorder(hvdcLine, country), false);
        outBciNetPosition += borderFlow;
    }

    private void handleLine(final Line line) {
        updatePositions(getBorderFlow(line, country), getCountryOnBorder(line, country), true);
    }

    private void handleDanglingLine(final DanglingLine danglingLine) {
        final double borderFlow = LoadFlowUtils.getBorderFlow(danglingLine, componentMode);
        final Country countryOnOtherSide = otherCountry(danglingLine);

        updatePositions(borderFlow, countryOnOtherSide, !isVirtualHubDanglingLine(danglingLine, virtualHubList));

        if (isVirtualHubDanglingLine(danglingLine, virtualHubList)) {
            outBciNetPosition += borderFlow;
            addToVirtualHubExchange(danglingLine.getPairingKey(), borderFlow);
        } else if (!bciCountries.contains(countryOnOtherSide)) {
            outBciNetPosition += borderFlow;
        }
    }

    private void updatePositions(final double borderFlow,
                                 final Country countryOnOtherSide,
                                 final boolean updateWithoutVirtualHub) {
        addToBorderExchange(borderFlow, countryOnOtherSide);
        globalPosition += borderFlow;
        if (updateWithoutVirtualHub) {
            globalPositionNoVh += borderFlow;
        }

        if (areBothInRegions(country, countryOnOtherSide)) {
            regionPosition += borderFlow;
            if (updateWithoutVirtualHub) {
                regionPositionNoVh += borderFlow;
            }
        }
    }

    private void addToVirtualHubExchange(final String hubName,
                                         final double flow) {
        virtualHubsExchanges.computeIfPresent(hubName, (h, f) -> f + flow);
        virtualHubsExchanges.putIfAbsent(hubName, flow);
    }

    private Bus getVirtualHubBus(final Substation substation) {
        if (country == substation.getNullableCountry()) {
            final List<String> virtualHubsNodeNames = virtualHubList.stream().map(VirtualHubRecord::getNodeName).toList();
            for (final String virtualHubName : virtualHubsNodeNames) {
                for (final VoltageLevel voltageLevel : substation.getVoltageLevels()) {
                    for (final Bus bus : voltageLevel.getBusBreakerView().getBuses()) {
                        if (virtualHubName.equals(bus.getId())) {
                            return bus;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void addToBorderExchange(final double flow,
                                     final Country country) {
        if (country != null) {
            globalDetailedExchanges.computeIfPresent(country.name(), (z, exchange) -> exchange + flow);
            globalDetailedExchanges.putIfAbsent(country.name(), flow);
        }
    }

    private Country otherCountry(final DanglingLine danglingLine) {
        final Country danglingLineCountry = BordersUtils.getCountry(danglingLine);
        final Optional<XnodeConfig> optionalXnode = getDanglingLineXnode(danglingLine);

        if (optionalXnode.isEmpty()) {
            LOGGER.warn("Could not find dangling line UCTE code: '{}' in xnodes config file. Considering it in outbci net position", danglingLine.getPairingKey());
        } else {
            final XnodeConfig xnode = optionalXnode.get();
            if (isInvalidDeXnode(xnode)) {
                LOGGER.warn("In the X-node configuration, {} is affected to area DE without valid subarea", xnode.getName());
            } else {
                final Country area1 = Country.valueOf(xnode.getArea1());
                final String subArea1 = xnode.getSubarea1();
                final Country area2 = Country.valueOf(xnode.getArea2());
                final String subArea2 = xnode.getSubarea2();

                updateIfKosovoXnode(xnode);

                if (areSameArea(danglingLineCountry, area1, subArea1)) {
                    return getCountry(subArea2 != null ? subArea2 : xnode.getArea2());
                } else if (areSameArea(danglingLineCountry, area2, subArea2)) {
                    return getCountry(subArea1 != null ? subArea1 : xnode.getArea1());
                } else {
                    LOGGER.warn("Error in xnodes configuration file : the node {} is not associated to country {}", xnode.getName(), danglingLineCountry);
                }
            }
        }
        return null;
    }

    private Optional<XnodeConfig> getDanglingLineXnode(final DanglingLine danglingLine) {
        return xnodeList.stream()
                .filter(xnode -> xnode.getName().trim().equals(danglingLine.getPairingKey()))
                .findFirst();
    }

    //In the xnode config, the xnodes with area DE should have a subarea to differentiate DE and DK
    private static boolean isInvalidDeXnode(final XnodeConfig xnode) {
        return DE.name().equals(xnode.getArea1()) && !GermanTso.includes(xnode.getSubarea1()) && !DANISH_TSO.equals(xnode.getSubarea1()) ||
               DE.name().equals(xnode.getArea2()) && !GermanTso.includes(xnode.getSubarea2()) && !DANISH_TSO.equals(xnode.getSubarea2());
    }

    private static boolean areSameArea(final Country country,
                                       final Country area,
                                       final String subArea) {
        return country == DE && isGermanArea(area, subArea)
               || country == DK && isDanishArea(area, subArea)
               || country != DE && country != DK && country == area;
    }

    private static boolean isGermanArea(final Country area,
                                        final String subArea) {
        return area == DE && GermanTso.includes(subArea);
    }

    private static boolean isDanishArea(final Country area,
                                        final String subArea) {
        return area == DE && DANISH_TSO.equals(subArea);
    }

    private static void updateIfKosovoXnode(final XnodeConfig xnode) {
        xnode.setArea1(CountryUtils.mapKsToXk(xnode.getArea1()));
        xnode.setArea2(CountryUtils.mapKsToXk(xnode.getArea2()));
    }

    private boolean isCountryInRegionConfig() {
        return regionConfiguration.getAreasIn().containsKey(country.toString());
    }

    private boolean areBothInRegions(final Country countryFrom,
                                     final Country countryTo) {
        if (countryFrom == null || countryTo == null) {
            return false;
        }
        return regionConfiguration.getAreasIn().keySet()
                .stream()
                .map(Country::valueOf)
                .collect(toSet())
                .containsAll(List.of(countryFrom, countryTo));
    }
}

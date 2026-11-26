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
import com.farao_community.farao.ce_merging.common.util.CountryCodeUtils;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.util.CountryCodeUtils.getCountryFromCode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.getComponentMode;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.isConnected;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getBorderFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getCountry;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getCountryOnOtherSide;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isBoundaryLineBorderOf;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isBranchBorderOf;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isHvdcLineBorderOf;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInCountry;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isPairedWithVirtualHub;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.zeroIfNaN;
import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.streamIterable;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;
import static java.util.stream.Collectors.toSet;

public final class CountryNetPositionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CountryNetPositionHandler.class);
    private final Map<String, Double> globalDetailedExchanges;
    private final Map<String, Double> virtualHubsExchanges;
    private final RegionConfiguration regionConfiguration;
    private final Network network;
    private final Country country;
    private final List<VirtualHubRecord> virtualHubList;
    private final List<XnodeConfig> xnodeList;
    private final LoadFlowParameters.ComponentMode componentMode;
    private final List<Country> bciCountries;
    private final Set<Country> inCountries;
    private double globalPosition;
    private double globalPositionNoVh;
    private double regionPosition;
    private double regionPositionNoVh;
    private double outBciNetPosition;

    private CountryNetPositionHandler(final RegionConfiguration regionConfiguration,
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

        this.inCountries = regionConfiguration.getAreasIn()
                .keySet()
                .stream()
                .map(Country::valueOf)
                .collect(toSet());
    }

    public static NetPositions computeCountryNetPositions(final Country country,
                                                          final Network network,
                                                          final Configurations configurations) {
        final CountryNetPositionHandler handler = new CountryNetPositionHandler(configurations.getRegionConfiguration(),
                                                                                network,
                                                                                country,
                                                                                configurations.getVirtualHubList(),
                                                                                configurations.getXnodeList(),
                                                                                getComponentMode(configurations.getLoadFlowParameters()));

        return handler.computeNetPositions();
    }

    private NetPositions computeNetPositions() {
        LOGGER.info("------ Computing net position of {} ({})", country.name(), country.getFullName());
        fillNetPositionsFromBoundaryLines();
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
        xnode.setArea1(CountryCodeUtils.mapKsToXk(xnode.getArea1()));
        xnode.setArea2(CountryCodeUtils.mapKsToXk(xnode.getArea2()));
    }

    private double sumOverCountry(final Stream<? extends Injection<?>> injectionStream) {
        return injectionStream
                .filter(isInCountry(country).and(isConnected(componentMode)))
                .map(Injection::getTerminal)
                .map(Terminal::getP)
                .mapToDouble(NetworkUtil::zeroIfNaN)
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

    private void fillNetPositionsFromBoundaryLines() {
        network.getBoundaryLineStream()
                .filter(isBoundaryLineBorderOf(country))
                .forEach(this::handleBoundaryLine);
    }

    private void handleVirtualHubBus(final Bus bus) {
        final double flow = zeroIfNaN(bus.getP());
        LOGGER.info("[{}] Adding virtual hub {} {} MW flow to global NP without VH: {} => {} MW",
                    country.name(), bus.getId(), (int) flow, (int) globalPosition, (int) (globalPosition + flow));
        globalPosition += flow;
        LOGGER.info("[{}] Adding virtual hub {} {} MW flow to outBciNetPosition: {} => {} MW",
                    country.name(), bus.getId(), (int) flow, (int) outBciNetPosition, (int) (outBciNetPosition + flow));
        outBciNetPosition += flow;

        addToVirtualHubExchange(bus.getId(), flow);

        if (isCountryInRegionConfig()) {
            LOGGER.info("[{}] Adding virtual hub {} {} MW flow to region NP without VH: {} => {} MW",
                        country.name(), bus.getId(), (int) flow, (int) regionPosition, (int) (regionPosition + flow));
            regionPosition += flow;
        }
    }

    private void handleHvdcLine(final HvdcLine hvdcLine) {
        double borderFlow = getBorderFlow(hvdcLine, country);
        final String updatedPositions = updatePositions(borderFlow, getCountryOnOtherSide(hvdcLine, country), false);
        LOGGER.info("[{}] HVDC line {}: {} and to outBciNetPosition", country.name(), hvdcLine.getId(), updatedPositions);
        outBciNetPosition += borderFlow;
    }

    private void handleLine(final Line line) {
        final String updatedPositions = updatePositions(getBorderFlow(line, country), getCountryOnOtherSide(line, country), true);
        LOGGER.info("[{}] Line {}: {}", country.name(), line.getId(), updatedPositions);
    }

    private void handleBoundaryLine(final BoundaryLine boundaryLine) {
        final double borderFlow = LoadFlowUtils.getBorderFlow(boundaryLine, componentMode);
        final Country countryOnOtherSide = otherCountry(boundaryLine);

        final String updatedPositions = updatePositions(borderFlow, countryOnOtherSide, !isPairedWithVirtualHub(boundaryLine, virtualHubList));
        LOGGER.info("[{}] Boundary line {}: {}", country.name(), boundaryLine.getId(), updatedPositions);
        final String xNode = boundaryLine.getPairingKey();

        if (isPairedWithVirtualHub(boundaryLine, virtualHubList)) {
            LOGGER.info("Adding boundary line {} {} MW flow to outBciNetPosition: {} => {} MW",
                        xNode, (int) borderFlow, (int) outBciNetPosition, (int) (outBciNetPosition + borderFlow));
            outBciNetPosition += borderFlow;
            addToVirtualHubExchange(xNode, borderFlow);
        } else if (!bciCountries.contains(countryOnOtherSide)) {
            LOGGER.info("Adding X-Node {} {} MW flow to outBciNetPosition: {} => {} MW",
                        xNode, (int) borderFlow, (int) outBciNetPosition, (int) (outBciNetPosition + borderFlow));
            outBciNetPosition += borderFlow;
        }
    }

    private String updatePositions(final double borderFlow,
                                   final Country countryOnOtherSide,
                                   final boolean updateWithoutVirtualHub) {
        addToBorderExchange(borderFlow, countryOnOtherSide);
        final StringBuilder sb = new StringBuilder("Adding ")
                .append((int) borderFlow)
                .append(" MW to global NP with VH (")
                .append((int) globalPosition)
                .append(" => ")
                .append((int) (globalPosition + borderFlow))
                .append(" MW)");

        globalPosition += borderFlow;
        if (updateWithoutVirtualHub) {
            sb.append(" & without (")
                    .append((int) globalPositionNoVh)
                    .append(" => ")
                    .append((int) (globalPositionNoVh + borderFlow))
                    .append(" MW)");

            globalPositionNoVh += borderFlow;
        }

        if (areBothInRegions(country, countryOnOtherSide)) {
            sb.append(", and to region NP with VH (").append((int) regionPosition)
                    .append(" => ")
                    .append((int) (regionPosition + borderFlow))
                    .append(" MW)");
            regionPosition += borderFlow;
            if (updateWithoutVirtualHub) {
                sb.append(" & without (")
                        .append((int) regionPositionNoVh)
                        .append(" => ")
                        .append((int) (regionPositionNoVh + borderFlow))
                        .append(" MW)");
                regionPositionNoVh += borderFlow;
            }
        }

        return sb.toString();
    }

    private void addToVirtualHubExchange(final String hubName,
                                         final double flow) {
        LOGGER.info("[{}] Set exchange with virtual hub {} to {} MW", country.name(), hubName, (int) flow);
        virtualHubsExchanges.computeIfPresent(hubName, (h, f) -> f + flow);
        virtualHubsExchanges.putIfAbsent(hubName, flow);
    }

    private Bus getVirtualHubBus(final Substation substation) {
        if (country != substation.getNullableCountry()) {
            return null;
        } else {
            return virtualHubList.stream()
                    .map(VirtualHubRecord::getNodeName)
                    .flatMap(name -> getSubstationBuses(substation).filter(bus -> bus.getId().equals(name)))
                    .findFirst()
                    .orElse(null);
        }

    }

    private Stream<Bus> getSubstationBuses(final Substation substation) {
        return streamIterable(substation.getVoltageLevels())
                .flatMap(vl -> streamIterable(vl.getBusBreakerView().getBuses()));
    }

    private void addToBorderExchange(final double flow,
                                     final Country country) {
        if (country != null) {
            LOGGER.info("Adding {} MW to globalDetailedExchanges with {}", (int) flow, country.getFullName());
            globalDetailedExchanges.computeIfPresent(country.name(), (z, exchange) -> exchange + flow);
            globalDetailedExchanges.putIfAbsent(country.name(), flow);
        }
    }

    private Country otherCountry(final BoundaryLine boundaryLine) {
        final Country boundaryLineCountry = getCountry(boundaryLine);
        final Optional<XnodeConfig> optionalXnode = getBoundaryLineXnode(boundaryLine);

        if (optionalXnode.isEmpty()) {
            LOGGER.warn("Could not find boundary line UCTE code: '{}' in xnodes config file. Considering it in outbci net position", boundaryLine.getPairingKey());
            return null;
        }

        final XnodeConfig xnode = optionalXnode.get();

        if (isInvalidDeXnode(xnode)) {
            LOGGER.warn("In the X-node configuration, {} is affected to area DE without valid subarea", xnode.getName());
            return null;
        }
        updateIfKosovoXnode(xnode);

        final Country area1 = Country.valueOf(xnode.getArea1());
        final String subArea1 = xnode.getSubarea1();
        final Country area2 = Country.valueOf(xnode.getArea2());
        final String subArea2 = xnode.getSubarea2();

        if (areSameArea(boundaryLineCountry, area1, subArea1)) {
            return getCountryFromCode(subArea2 != null ? subArea2 : xnode.getArea2());
        } else if (areSameArea(boundaryLineCountry, area2, subArea2)) {
            return getCountryFromCode(subArea1 != null ? subArea1 : xnode.getArea1());
        } else {
            LOGGER.warn("Error in xnodes configuration file : the node {} is not associated to country {}", xnode.getName(), boundaryLineCountry);
            return null;
        }
    }

    private Optional<XnodeConfig> getBoundaryLineXnode(final BoundaryLine boundaryLine) {
        return xnodeList.stream()
                .filter(xnode -> xnode.getName().trim().equals(boundaryLine.getPairingKey()))
                .findFirst();
    }

    private boolean isCountryInRegionConfig() {
        return regionConfiguration.getAreasIn().containsKey(country.toString());
    }

    private boolean areBothInRegions(final Country countryFrom,
                                     final Country countryTo) {
        if (countryFrom == null || countryTo == null) {
            return false;
        }
        return inCountries.containsAll(List.of(countryFrom, countryTo));
    }
}

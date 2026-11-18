/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.ucte.converter.UcteExporterContext;
import com.powsybl.ucte.network.UcteNetwork;
import com.powsybl.ucte.network.UcteNode;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.INTERNAL_BIDDING_ZONE_PROPERTY_KEY;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.UNMAPPED;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class InternalBiddingZoneImportPostProcessor {//implements UcteExportPostProcessor {

    public static final String NAME = "ITA-BZ-POST-PROCESSOR";
    private static final Function<Bus, String> BIDDING_ZONES_GROUP = b -> Optional.ofNullable(b.getProperty(INTERNAL_BIDDING_ZONE_PROPERTY_KEY)).orElse(UNMAPPED);
    private static final String BZ_SUMMARY_PREFIX = "Bidding zones grouped in nodes of IT: ";
    private static final String BZ_INFO_FORMAT = "%s %d Nodes First Node: %s, Last Node: %s";

    //@Override
    public String getName() {
        return NAME;
    }

    private Map<String, List<Bus>> nodesByBiddingZone(final Network network) {
        final Map<String, List<Bus>> busesByBiddingZone = network.getBusBreakerView().getBusStream().collect(groupingBy(BIDDING_ZONES_GROUP));
        busesByBiddingZone.forEach((bz, buses) -> buses.sort(comparing(Bus::getId)));

        return busesByBiddingZone;
    }

    private Map<String, String> biddingZoneByNodeName(final Network network) {
        final Map<String, List<Bus>> busesByBiddingZone = network.getBusBreakerView().getBusStream().collect(groupingBy(BIDDING_ZONES_GROUP));
        busesByBiddingZone.forEach((bz, buses) -> buses.sort(comparing(Bus::getId)));

        return network.getBusBreakerView().getBusStream().collect(toMap(Bus::getId, BIDDING_ZONES_GROUP));
    }

    //@Override
    public void process(Network network,
                        UcteNetwork ucteNetwork,
                        UcteExporterContext context) {
        final Map<String, List<Bus>> nodesByBz = nodesByBiddingZone(network);
        final List<String> headerComments = ucteNetwork.getComments();
        final Map<String, Integer> nodeCountByBz = new HashMap<>();

        nodesByBz.forEach((biddingZone, busList) -> {
            final int nodeCount = busList.size();
            headerComments.add(BZ_INFO_FORMAT.formatted(biddingZone, nodeCount, busList.getFirst(), busList.getLast()));
            nodeCountByBz.put(biddingZone, nodeCount);
        });

        headerComments.addFirst(BZ_SUMMARY_PREFIX + nodeCountByBz.entrySet()
                                        .stream()
                                        .map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                                        .collect(Collectors.joining(", "))
        );
    }

    //@Override
    public Optional<Comparator<UcteNode>> getNodeComparator(Network network,
                                                            UcteNetwork ucteNetwork,
                                                            UcteExporterContext context) {
        return Optional.of(getZoneComparator(network).thenComparing(naturalOrder()));
    }

    private Comparator<UcteNode> getZoneComparator(Network network) {
        final Map<String, String> bzByNodeName = biddingZoneByNodeName(network);
        return (n1, n2) -> getZoneOf(n1, bzByNodeName).compareTo(getZoneOf(n2, bzByNodeName));
    }

    private String getZoneOf(final UcteNode node,
                             final Map<String, String> bzByNodeName) {
        return bzByNodeName.get(node.getCode().toString());
    }

}

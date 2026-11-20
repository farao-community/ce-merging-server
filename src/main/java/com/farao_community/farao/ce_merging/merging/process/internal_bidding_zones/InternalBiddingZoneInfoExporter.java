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

import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.INTERNAL_BIDDING_ZONE_PROPERTY_KEY;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.UNMAPPED;
import static com.powsybl.ucte.network.UcteCountryCode.IT;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class InternalBiddingZoneInfoExporter {//implements UcteExportPostProcessor {

    public static final String NAME = "ITA-BZ-POST-PROCESSOR";

    //@Override
    public String getName() {
        return NAME;
    }

    private static String getZone(final Bus bus) {
        return Optional.ofNullable(bus.getProperty(INTERNAL_BIDDING_ZONE_PROPERTY_KEY)).orElse(UNMAPPED);
    }

    private Map<String, List<Bus>> nodesByBiddingZone(final Network network) {
        final Map<String, List<Bus>> busesByBiddingZone = network.getBusBreakerView().getBusStream()
                .collect(groupingBy(InternalBiddingZoneInfoExporter::getZone));
        busesByBiddingZone.forEach((bz, buses) -> buses.sort(comparing(Bus::getId)));

        return busesByBiddingZone;
    }

    private Map<String, String> biddingZoneByNodeName(final Network network) {
        final Map<String, List<Bus>> busesByBiddingZone = network.getBusBreakerView().getBusStream()
                .collect(groupingBy(InternalBiddingZoneInfoExporter::getZone));
        busesByBiddingZone.forEach((bz, buses) -> buses.sort(comparing(Bus::getId)));

        return network.getBusBreakerView().getBusStream().collect(toMap(Bus::getId,
                                                                        InternalBiddingZoneInfoExporter::getZone));
    }

    //@Override
    public void process(final Network network,
                        final UcteNetwork ucteNetwork,
                        final UcteExporterContext context) {
        final Map<String, List<Bus>> nodesByBz = nodesByBiddingZone(network);
        final List<String> comments = ucteNetwork.getComments();
        final Map<String, Integer> nodeCountByBz = new HashMap<>();

        nodesByBz.forEach((biddingZone, buses) -> {
            comments.add(getBiddingZoneComment(biddingZone, buses));
            nodeCountByBz.put(biddingZone, buses.size());
        });

        comments.addFirst(getHeaderComment(nodeCountByBz));
    }

    private String getBiddingZoneComment(final String biddingZone, final List<Bus> buses) {
        return "%s %d Nodes First Node: %s, Last Node: %s".formatted(
                biddingZone, buses.size(), buses.getFirst(), buses.getLast()
        );
    }

    private String getHeaderComment(final Map<String, Integer> nodeCountByBz) {
        return "Bidding zones grouped in nodes of IT: " + nodeCountByBz.entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                .collect(joining(", "));
    }


    //@Override
    public Optional<Comparator<UcteNode>> getNodeComparator(final Network network,
                                                            final UcteNetwork ucteNetwork,
                                                            final UcteExporterContext context) {
        return Optional.of(getZoneComparator(network).thenComparing(naturalOrder()));
    }

    private Comparator<UcteNode> getZoneComparator(final Network network) {
        final Map<String, String> bzByNodeName = biddingZoneByNodeName(network);
        // since we order within country blocks, both nodes are in the same country
        // if it is not Italy, N/A
        return (n1, n2) -> n1.getCode().getUcteCountryCode() != IT ? 0
                : getZoneOf(n1, bzByNodeName).compareTo(getZoneOf(n2, bzByNodeName));
    }

    private String getZoneOf(final UcteNode node,
                             final Map<String, String> bzByNodeName) {
        return bzByNodeName.get(node.getCode().toString());
    }

}

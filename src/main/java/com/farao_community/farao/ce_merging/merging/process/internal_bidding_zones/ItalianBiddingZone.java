/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.InternalTieLineType;

import java.util.Arrays;
import java.util.Objects;

public enum ItalianBiddingZone {
    INOR("10Y1001A1001A73I"),
    ICNO("10Y1001A1001A70O"),
    ICSO("10Y1001A1001A71M"),
    ISOU("10Y1001A1001A788"),
    ICAL("10Y1001C--00096J");

    private final String eic;

    ItalianBiddingZone(final String eic) {
        this.eic = eic;
    }

    public String getEic() {
        return eic;
    }

    public double getFlowSign(final InternalTieLineType line) {
        if (lineComesFrom(line, this)) {
            return 1;
        } else if (lineGoesTo(line, this)) {
            return -1;
        } else {
            return 0;
        }
    }

    public static boolean lineJoinsZones(final InternalTieLineType line,
                                         final ItalianBiddingZone bz1,
                                         final ItalianBiddingZone bz2) {
        return lineComesFrom(line, bz1) && lineGoesTo(line, bz2)
               || lineComesFrom(line, bz2) && lineGoesTo(line, bz1);
    }

    public static ItalianBiddingZone getByEic(final String eic) {
        return Arrays.stream(values()).filter(bz -> bz.getEic().equals(eic)).findAny()
                .orElseThrow(() -> new CeMergingException("%s does not correspond to an italian bidding zone"));
    }

    public static boolean isItalianBiddingZone(final String name) {
        return Arrays.stream(values()).anyMatch(bz -> bz.name().equals(name));
    }

    public static boolean isItalianBiddingZoneEic(final String eic) {
        return Arrays.stream(values()).anyMatch(bz -> bz.getEic().equals(eic));
    }

    private static boolean lineGoesTo(final InternalTieLineType line,
                                      final ItalianBiddingZone biddingZone) {
        return Objects.equals(line.getBiddingZoneTo().getValue(), biddingZone.name());
    }

    private static boolean lineComesFrom(final InternalTieLineType line,
                                         final ItalianBiddingZone biddingZone) {
        return Objects.equals(line.getBiddingZoneFrom().getValue(), biddingZone.name());
    }
}

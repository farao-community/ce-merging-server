/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;

import java.util.Arrays;

public enum ItalianBiddingZone {
    INOR("NORD", "10Y1001A1001A73I"),
    ICNO("CENTRO_NORD", "10Y1001A1001A70O"),
    ICSO("CENTRO_SUD", "10Y1001A1001A71M"),
    ISOU("SUD", "10Y1001A1001A788"),
    ICAL("CALABRIA", "10Y1001C--00096J");

    private final String italianName;
    private final String eic;

    ItalianBiddingZone(final String italianName,
                       final String eic) {
        this.italianName = italianName;
        this.eic = eic;
    }

    public String getItalianName() {
        return italianName;
    }

    public String getEic() {
        return eic;
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
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

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
}

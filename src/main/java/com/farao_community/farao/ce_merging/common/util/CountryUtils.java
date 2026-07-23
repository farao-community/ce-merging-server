/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.powsybl.iidm.network.Country;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_TSO;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

public final class CountryUtils {
    public static final String KOSOVO_CODE = "KS";
    public static final String KOSOVO_ISO_CODE = "XK";
    public static final String DK1_CODE = "DK1";
    public static final String DENMARK_CODE = "DK";

    private CountryUtils() {
    }

    public static String mapKsToXk(final String country) {
        return KOSOVO_CODE.equals(country) ? KOSOVO_ISO_CODE : country;
    }

    public static String mapXkToKs(final String country) {
        return KOSOVO_ISO_CODE.equals(country) ? KOSOVO_CODE : country;
    }

    public static Country getCountry(final String countryName) {
        if (GERMAN_TSO.contains(countryName)) {
            return DE;
        } else if (DANISH_TSO.equals(countryName)) {
            return DK;
        } else {
            return Country.valueOf(countryName);
        }
    }

    public static String mapDk1ToDk(final String country) {
        return DK1_CODE.equals(country) ? DENMARK_CODE : country;
    }
}

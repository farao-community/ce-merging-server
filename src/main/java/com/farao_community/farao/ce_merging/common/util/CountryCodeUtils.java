/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.Country;
import com.powsybl.ucte.network.UcteCountryCode;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

public final class CountryCodeUtils {
    public static final String DK1_CODE = "DK1";

    private CountryCodeUtils() {
    }

    public static String mapKsToXk(final String country) {
        return UcteCountryCode.KS.name().equals(country) ? Country.XK.name() : country;
    }

    public static Country getCountryFromCode(final String countryName) {
        if (GermanTso.includes(countryName)) {
            return DE;
        } else if (DANISH_TSO.equals(countryName)) {
            return DK;
        } else {
            return Country.valueOf(countryName);
        }
    }

    public static String mapDk1ToDk(final String country) {
        return DK1_CODE.equals(country) ? DK.name() : country;
    }
}

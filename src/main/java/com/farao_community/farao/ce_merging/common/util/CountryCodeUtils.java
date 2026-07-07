/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

public final class CountryCodeUtils {
    public static final String KOSOVO_CODE = "KS";
    public static final String KOSOVO_ISO_CODE = "XK";
    public static final String DK1_CODE = "DK1";
    public static final String DENMARK_CODE = "DK";

    private CountryCodeUtils() {
    }

    public static String mapKsToXk(String country) {
        return KOSOVO_CODE.equals(country) ? KOSOVO_ISO_CODE : country;
    }

    public static String mapXkToKs(String country) {
        return KOSOVO_ISO_CODE.equals(country) ? KOSOVO_CODE : country;
    }

    public static String mapDk1ToDk(String country) {
        return DK1_CODE.equals(country) ? DENMARK_CODE : country;
    }
}

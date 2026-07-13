/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process;

import com.farao_community.farao.ce_merging.common.util.JsonUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

public final class TargetNetPositionsImporter {

    private static final String NET_POSITION_LIST_FIELD = "netPositions";
    private static final String NET_POSITION_FIELD = "netPosition";
    private static final String AREA_FIELD = "area";

    private TargetNetPositionsImporter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Map<String, Double> getTargetNetPositionsAreasFromFile(final File inputFile) throws IOException {
        try (final FileInputStream fis = new FileInputStream(inputFile)) {
            return getTargetNetPositionsAreasFromFile(fis);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Double> getTargetNetPositionsAreasFromFile(final InputStream input) {
        return ((ArrayList<HashMap<String, Object>>) JsonUtils.read(Map.class, input)
            .get(NET_POSITION_LIST_FIELD))
            .stream()
            .collect(toMap(o -> (String) o.get(AREA_FIELD),
                           o -> (Double) o.get(NET_POSITION_FIELD)));
    }
}

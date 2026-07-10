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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public final class TargetNetPositionsImporter {

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
        return Stream.of(JsonUtils.read(Map.class, input).get("netPositions"))
            .map(data -> (HashMap<String, Object>) data)
            .collect(toMap(data -> (String) data.get("area"),
                           data -> (double) data.get("netPosition")));
    }
}

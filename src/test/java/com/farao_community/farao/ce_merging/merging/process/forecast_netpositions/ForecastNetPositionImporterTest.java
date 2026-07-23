/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.forecast_netpositions;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ForecastNetPositionImporterTest {

    private static final String NET_POSITION_ALEGRO_FILE_PATH = "src/test/resources/forecastNetPosition/test_npf_file.xml";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2020-03-15T23:00Z");
    private static final double DELTA = 0.;

    @Test
    public void importFromAlegroFileFirstTime() {
        ReferenceProgram referenceProgram = ForecastNetPositionImporter.importFromFile(NET_POSITION_ALEGRO_FILE_PATH, TARGET_DATE);
        List<ReferenceExchangeData> data = referenceProgram.getReferenceExchangeDataList();
        assertEquals(2, data.size());
        assertEquals("XXX", data.getFirst().getAreaInId());
        assertEquals("YYY", data.getFirst().getAreaOutId());
        assertEquals(-38, data.getFirst().getFlow(), DELTA);
        assertEquals("AAA", data.getLast().getAreaInId());
        assertEquals("BBB", data.getLast().getAreaOutId());
        assertEquals(-295, data.getLast().getFlow(), DELTA);

    }
}

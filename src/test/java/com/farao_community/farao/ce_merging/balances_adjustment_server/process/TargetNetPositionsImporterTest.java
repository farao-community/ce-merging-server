/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.balances_adjustment_server.process;

import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process.TargetNetPositionsImporter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TargetNetPositionsImporterTest {

    @Test
    void getReferenceNetPositionsAreasFromFileTest() throws IOException {
        File netPositionsFile = new File(getClass().getResource("/balances/NetPositions.json").getFile());
        Map<String, Double> netPositionNetworkAreas = TargetNetPositionsImporter.getTargetNetPositionsAreasFromFile(netPositionsFile);

        assertFalse(netPositionNetworkAreas.isEmpty());
        assertEquals(12, netPositionNetworkAreas.size());
    }
}

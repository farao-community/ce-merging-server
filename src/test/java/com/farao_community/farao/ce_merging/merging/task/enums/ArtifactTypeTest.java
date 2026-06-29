/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.enums;

import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ArtifactTypeTest {

    @Test
    void shouldExposeExpectedMetadata() {
        assertEquals("xnodesInformation.json", XNODES_INFORMATION_FILE.getFileName());
        assertEquals("xnodes-information", XNODES_INFORMATION_FILE.getLocation());
        assertEquals("cgmNetPositions.json", CGM_NET_POSITIONS_FILE.getFileName());
        assertEquals("cgm-net-positions", CGM_NET_POSITIONS_FILE.getLocation());
    }

}

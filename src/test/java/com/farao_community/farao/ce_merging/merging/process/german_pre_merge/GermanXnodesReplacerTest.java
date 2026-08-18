/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.german_pre_merge;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import test_utils.assertions.LineAssert;

import java.util.Map;

import static test_utils.assertions.NetworkAssert.assertThat;

class GermanXnodesReplacerTest {

    private static final String D2_LINE = "D2XXXX11 DXXXXX11 1";
    private static final String D4_LINE = "D4XXXX11 DXXXXX11 1";

    @Test
    void shouldReplaceXnodesWithLines() {
        final Network mergedGermanNetwork = Network.read(getClass().getResource("/german/mergeFakeFile.uct").getPath());
        GermanXnodesReplacer.replaceXnodesWithLines(mergedGermanNetwork);

        assertThat(mergedGermanNetwork)
                .hasBus("DXXXXX11")
                .doesNotHaveBus("XXXXXX11")
                .doesNotHaveLine("D2XXXX11 XXXXXX11 1")
                .hasLine(D2_LINE)
                .hasLine(D4_LINE);

        LineAssert.assertThat(mergedGermanNetwork.getLine(D2_LINE))
                .hasProperties(Map.of("elementName", "ELEMENT1"))
                .hasPermanentCurrentLimits(Map.of("tieLineLimits_1", 5051.));

        LineAssert.assertThat(mergedGermanNetwork.getLine(D4_LINE))
                .hasProperties(Map.of("elementName", "ELEMENT2"))
                .hasPermanentCurrentLimits(Map.of("tieLineLimits_2", 5051.));

    }

}

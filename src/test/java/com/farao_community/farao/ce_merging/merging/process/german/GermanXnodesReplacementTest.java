/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.german;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static test_utils.assertions.NetworkAssert.assertThat;

public class GermanXnodesReplacementTest {

    @Test
    public void shouldModifyPreMergedGermanIgm() {
        final Network mergedGermanNetwork = Network.read(getClass().getResource("/german/mergeFakeFile.uct").getPath());
        GermanXnodesReplacer.replaceXnodesWithLines(mergedGermanNetwork);

        assertThat(mergedGermanNetwork)
                .hasBus("DXXXXX11")
                .doesNotHaveBus("XXXXXX11")
                .doesNotHaveLine("D2XXXX11 XXXXXX11 1")
                .hasLineWithProperties("D2XXXX11 DXXXXX11 1", Map.of("elementName", "ELEMENT1"))
                .hasLineWithProperties("D4XXXX11 DXXXXX11 1", Map.of("elementName", "ELEMENT2"));

    }

}

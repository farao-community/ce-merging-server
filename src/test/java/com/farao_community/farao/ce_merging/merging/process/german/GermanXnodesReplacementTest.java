/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.german;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GermanXnodesReplacementTest {

    @Test
    public void shouldModifyPreMergedGermanIgm() {
        Network deIgm = Network.read(getClass().getResource("/german/mergeFakeFile.uct").getPath());
        GermanXnodesReplacer.replaceWithLines(deIgm);

        assertNull(deIgm.getBusBreakerView().getBus("XXXXXX11"));
        assertNotNull(deIgm.getBusBreakerView().getBus("DXXXXX11"));
        assertNull(deIgm.getLine("D2XXXX11 XXXXXX11 1"));
        assertNotNull(deIgm.getLine("D2XXXX11 DXXXXX11 1"));
        assertNotNull(deIgm.getLine("D4XXXX11 DXXXXX11 1"));
        assertEquals("ELEMENT1", deIgm.getLine("D2XXXX11 DXXXXX11 1").getProperty("elementName"));
        assertEquals("ELEMENT2", deIgm.getLine("D4XXXX11 DXXXXX11 1").getProperty("elementName"));
    }

}

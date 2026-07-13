/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.balances_adjustment.process;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static test_utils.CeTestUtils.stringPathOf;

class ZonalDataImporterTest {
    private static final OffsetDateTime PROCESS_TARGET_DATE = OffsetDateTime.parse("2021-07-22T22:30Z");

    @Test
    void shouldImportGlsk() throws IOException {
        final File glskFile = new File(getClass().getResource("/balances/20210723-F226-v1.xml").getFile());
        final Network network = Network.read(stringPathOf("balances/20210723_0030_2D1_UC5_F100_CORESO.uct"));
        final Map<String, Scalable> zonalData = ZonalDataImporter.getZonalDataFromGlsk(glskFile,
                                                                                       network,
                                                                                       PROCESS_TARGET_DATE);

        assertThat(zonalData).isNotEmpty();
    }
}

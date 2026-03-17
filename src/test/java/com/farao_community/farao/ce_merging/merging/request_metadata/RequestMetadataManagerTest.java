/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.request_metadata;

import com.farao_community.farao.ce_merging.common.exception.InvalidTaskException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.RequestMetadata;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.time.ZoneOffset;

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.pathOf;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringContentOf;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringPathOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestMetadataManagerTest {

    private static final String INPUTS = "request-metadata/inputs/";
    private static final String METADATA = "request-metadata/metadata.json";
    private static final ZoneOffset PARIS_WINTER_OFFSET = ZoneOffset.of("+02:00");

    @Test
    void shouldCalculateRealOffset() throws FileNotFoundException {
        final RequestMetadata reqMd = JsonUtils.read(RequestMetadata.class,
                                                     stringPathOf(METADATA));
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      reqMd);
        assertEquals(PARIS_WINTER_OFFSET, mgr.getRealRequestOffset());
    }

    @Test
    void shouldNotThrowIfAllInputsAvailable() {

        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      stringContentOf(METADATA));
        assertDoesNotThrow(() -> mgr.checkIfAllInputsAvailable(pathOf(INPUTS)));
    }

    @Test
    void shouldThrowIfAnyInputMissing() throws FileNotFoundException {
        final RequestMetadata reqMd = JsonUtils.read(RequestMetadata.class,
                                                     stringPathOf(METADATA));
        reqMd.getData().getAttributes().getInputs().setExternalConstraintsFilePath("not/existing");
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      reqMd);

        final Path incomplete = pathOf(INPUTS);
        assertThrows(InvalidTaskException.class, () -> mgr.checkIfAllInputsAvailable(incomplete));
    }
}

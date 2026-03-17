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

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.pathOfTestFile;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringContentOfTestFile;
import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringPathOfTestFile;
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
                                                     stringPathOfTestFile(METADATA));
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOfTestFile(INPUTS),
                                                                      reqMd);
        assertEquals(PARIS_WINTER_OFFSET, mgr.getRealRequestOffset());
    }

    @Test
    void shouldNotThrowIfAllInputsAvailable() {

        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOfTestFile(INPUTS),
                                                                      stringContentOfTestFile(METADATA));
        assertDoesNotThrow(() -> mgr.checkIfAllInputsAvailable(pathOfTestFile(INPUTS)));
    }

    @Test
    void shouldThrowIfAnyInputMissing() throws FileNotFoundException {
        final RequestMetadata reqMd = JsonUtils.read(RequestMetadata.class,
                                                     stringPathOfTestFile(METADATA));
        reqMd.getData().getAttributes().getInputs().setExternalConstraintsFilePath("not/existing");
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOfTestFile(INPUTS),
                                                                      reqMd);

        final Path incomplete = pathOfTestFile(INPUTS);
        assertThrows(InvalidTaskException.class, () -> mgr.checkIfAllInputsAvailable(incomplete));
    }
}

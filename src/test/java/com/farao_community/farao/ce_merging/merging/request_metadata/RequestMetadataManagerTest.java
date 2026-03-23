/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.request_metadata;

import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.Data;
import com.farao_community.farao.ce_merging.merging.request_metadata.model.RequestMetadata;
import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.io.FileNotFoundException;
import java.time.ZoneOffset;
import java.util.InputMismatchException;

import static com.farao_community.farao.ce_merging.merging.task.entities.enums.TaskStatus.CREATED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.pathOf;
import static test_utils.CeTestUtils.stringContentOf;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.taskWithIdAndStatus;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class RequestMetadataManagerTest {

    private static final String INPUTS = "request-metadata/inputs/";
    private static final String METADATA = "request-metadata/metadata.json";
    private static final String METADATA_WITHOUT_RECESSIVITY_PARAMETERS = "request-metadata/metadata_no_rp.json";
    private static final ZoneOffset PARIS_WINTER_OFFSET = ZoneOffset.of("+02:00");

    @Test
    void shouldCalculateRealOffset() throws FileNotFoundException {
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      getMetadata());
        assertEquals(PARIS_WINTER_OFFSET, mgr.getParisRequestOffset());
    }

    @Test
    void shouldNotThrowIfAllInputsAvailable() {

        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      stringContentOf(METADATA));
        assertDoesNotThrow(() -> mgr.checkIfAllInputsAvailable(pathOf(INPUTS)));

        final RequestMetadataManager mgrNoRp = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                          stringContentOf(METADATA_WITHOUT_RECESSIVITY_PARAMETERS));
        assertDoesNotThrow(() -> mgrNoRp.checkIfAllInputsAvailable(pathOf(INPUTS)));
    }

    @Test
    void shouldThrowIfAnyInputMissing() throws FileNotFoundException {
        final RequestMetadata reqMd = getMetadata();
        reqMd.getData().getAttributes().getInputs().setExternalConstraintsFilePath("not/existing");
        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      reqMd);

        assertThatThrownBy(() -> mgr.checkIfAllInputsAvailable(pathOf(INPUTS)))
            .isTaskException()
            .hasMessageContaining("not/existing");
    }

    @Test
    void shouldThrowIfIgmHasInvalidName() throws FileNotFoundException {
        final RequestMetadata reqMd = getMetadata();
        reqMd.getData()
            .getAttributes()
            .getInputs()
            .getIgms()
            .forEach(igm -> igm.getIgmFile().setPath("inputs/EC.EC"));

        final RequestMetadataManager mgr = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                      reqMd);

        assertThatThrownBy(() -> mgr.feedTaskData(taskWithIdAndStatus(1, CREATED)))
            .isInstanceOf(InputMismatchException.class);
    }

    @Test
    void shouldThrowIfWrongMetadata() {
        assertThatThrownBy(() -> new RequestMetadataManager(stringPathOf(INPUTS), stringContentOf(INPUTS)))
            .isServiceException()
            .hasMessage("Invalid request metadata");
    }

    @Test
    void dataShouldHaveAccessors() {
        GetterSetterVerifier
            .forClass(Data.class)
            .verify();
    }

    private RequestMetadata getMetadata() throws FileNotFoundException {
        return JsonUtils.read(RequestMetadata.class,
                              stringPathOf(METADATA));
    }
}

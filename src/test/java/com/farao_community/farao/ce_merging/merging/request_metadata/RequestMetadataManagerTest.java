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

import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.CREATED;
import static com.farao_community.farao.ce_merging.merging.enums.TaskStatus.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.ID_1;
import static test_utils.CeTestUtils.pathOf;
import static test_utils.CeTestUtils.stringContentOf;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.taskWithIdAndStatus;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class RequestMetadataManagerTest {

    private static final String INPUTS = "request-metadata/inputs/";
    private static final String METADATA = "request-metadata/metadata.json";
    private static final String METADATA_WITHOUT_RECESSIVITY_PARAMETERS = "request-metadata/metadata_no_rp.json";
    private static final ZoneOffset PARIS_SUMMER_OFFSET = ZoneOffset.of("+02:00");

    @Test
    void shouldReturnRealOffsetFromRequestMetadata() throws FileNotFoundException {
        assertEquals(PARIS_SUMMER_OFFSET, getRequestMetadataManager(getMetadata()).getParisRequestOffset());
    }

    @Test
    void shouldNotThrowIfAllInputsAvailable() {

        assertDoesNotThrow(() -> getRequestMetadataManager(stringContentOf(METADATA)).checkIfAllInputsAvailable(pathOf(INPUTS)));

        final RequestMetadataManager managerWithoutRecessivityParameters = new RequestMetadataManager(stringPathOf(INPUTS),
                                                                                                      stringContentOf(METADATA_WITHOUT_RECESSIVITY_PARAMETERS));
        assertDoesNotThrow(() -> managerWithoutRecessivityParameters.checkIfAllInputsAvailable(pathOf(INPUTS)));
        assertDoesNotThrow(() -> managerWithoutRecessivityParameters.feedTaskData(taskWithIdAndStatus(ID_1, SUCCESS)));
    }

    @Test
    void shouldThrowIfAnyInputMissing() throws FileNotFoundException {
        final RequestMetadata metadata = getMetadata();
        metadata.getData().getAttributes().getInputs().setExternalConstraintsFilePath("not/existing");

        assertThatThrownBy(() -> getRequestMetadataManager(metadata).checkIfAllInputsAvailable(pathOf(INPUTS)))
            .isTaskException()
            .hasMessageContaining("not/existing");
    }

    @Test
    void shouldThrowIfIgmHasInvalidName() throws FileNotFoundException {
        final RequestMetadata metadata = getMetadata();
        metadata.getData()
            .getAttributes()
            .getInputs()
            .getIgms()
            .forEach(igm -> igm.getIgmFile().setPath("inputs/EC.EC"));

        assertThatThrownBy(() -> getRequestMetadataManager(metadata).feedTaskData(taskWithIdAndStatus(ID_1, CREATED)))
            .isInstanceOf(InputMismatchException.class);
    }

    @Test
    void shouldThrowIfWrongMetadata() {
        assertThatThrownBy(() -> getRequestMetadataManager(stringContentOf(INPUTS)))
            .isValidServiceException()
            .hasMessage("Invalid request metadata");
    }

    @Test
    void dataShouldHaveAccessors() {
        GetterSetterVerifier
            .forClass(Data.class)
            .verify();
    }

    private RequestMetadata getMetadata() throws FileNotFoundException {
        return JsonUtils.read(RequestMetadata.class, stringPathOf(METADATA));
    }

    private RequestMetadataManager getRequestMetadataManager(final String jsonContent) {
        return new RequestMetadataManager(stringPathOf(INPUTS), jsonContent);
    }

    private RequestMetadataManager getRequestMetadataManager(final RequestMetadata metadata) {
        return new RequestMetadataManager(stringPathOf(INPUTS), metadata);
    }
}

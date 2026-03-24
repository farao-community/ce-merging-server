/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class IgmTypeTest {

    @Test
    void shouldInstantiateWithCorrectCode() {
        List.of("SN", "FO", "2D").forEach(code -> assertDoesNotThrow(() -> IgmType.fromTypeCode(code)));
    }

    @Test
    void shouldFailWithInvalidCode() {
        assertThatThrownBy(() -> IgmType.fromTypeCode("i am not valid"))
            .isServiceException()
            .hasMessage("Type code 'i am not valid' not recognized");
    }

}

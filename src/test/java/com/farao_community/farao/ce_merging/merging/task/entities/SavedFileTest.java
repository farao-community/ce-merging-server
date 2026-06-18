/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class SavedFileTest {

    @Test
    void shouldNotFeedInvalidPath() {
        final SavedFile file = new SavedFile();
        assertThatThrownBy(() -> file.feedPathAndName(""))
            .isValidServiceException()
            .hasMessage("Path cannot be empty");

        assertThatThrownBy(() -> file.feedPathAndName((Path) null))
            .isValidServiceException()
            .hasMessage("Path cannot be empty");
    }

}

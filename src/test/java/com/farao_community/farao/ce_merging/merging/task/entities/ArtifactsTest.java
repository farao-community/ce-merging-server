/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.merging.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static org.assertj.core.api.Assertions.assertThat;

class ArtifactsTest {

    @Test
    void shouldManipulateInternalMap() {
        final Artifacts artifacts = new Artifacts();
        assertThat(artifacts.getFile(XNODES_INFORMATION_FILE)).isNull();
        artifacts.putFile(XNODES_INFORMATION_FILE, new SavedFile());
        assertThat(artifacts.getFile(XNODES_INFORMATION_FILE)).isNotNull();
    }

}

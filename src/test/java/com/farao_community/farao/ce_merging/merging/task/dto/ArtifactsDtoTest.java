/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import org.junit.jupiter.api.Test;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static org.assertj.core.api.Assertions.assertThat;

class ArtifactsDtoTest {
    @Test
    void shouldManipulateInternalMap() {
        final ArtifactsDto artifactsDto = new ArtifactsDto();
        assertThat(artifactsDto.getLocation(XNODES_INFORMATION_FILE)).isNull();
        artifactsDto.putLocation(XNODES_INFORMATION_FILE, ".");
        assertThat(artifactsDto.getLocation(XNODES_INFORMATION_FILE)).isNotNull();
    }
}

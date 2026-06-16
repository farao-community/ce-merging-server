/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.ListUtils.clonedList;
import static com.farao_community.farao.ce_merging.common.util.ListUtils.deNulledList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ListUtilsTest {

    private static final List<String> TEST_LIST = new ArrayList<>(List.of(new String[]{"hello"}));

    @Test
    void shouldTransformNullListToNotNull() {
        final List<?> fromNull = deNulledList(null);
        assertNotNull(fromNull);
        assertThat(fromNull).isEmpty();
        assertEquals(TEST_LIST.getFirst(), deNulledList(TEST_LIST).getFirst());
    }

    @Test
    void shouldCloneListWithoutReference() {
        final List<String> clone = clonedList(TEST_LIST);
        assertEquals(TEST_LIST.getFirst(), clone.getFirst());
        clone.add("goodbye");
        assertEquals(1, TEST_LIST.size());
        assertEquals(new ArrayList<>(), clonedList(null));
    }
}

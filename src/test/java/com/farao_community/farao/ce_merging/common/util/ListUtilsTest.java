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

    @Test
    void shouldTransformNullListToNotNull() {
        final List<String> list = new ArrayList<>(List.of(new String[]{"hello"}));
        final List<?> fromNull = deNulledList(null);
        assertNotNull(fromNull);
        assertThat(fromNull).isEmpty();
        assertEquals(list.getFirst(), deNulledList(list).getFirst());
    }

    @Test
    void shouldCloneListWithoutReference() {
        final List<String> list = new ArrayList<>(List.of(new String[]{"hello"}));
        final List<String> clone = clonedList(list);
        assertEquals(list.getFirst(), clone.getFirst());
        list.add("goodbye");
        assertEquals(1, clone.size());
        assertEquals(new ArrayList<>(), clonedList(null));
    }
}

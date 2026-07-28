/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.enums;

import java.util.Arrays;

public enum GermanTso {
    D2,
    D4,
    D6,
    D7,
    D8;

    public static boolean includes(final String tsoCode) {
        return Arrays.stream(values()).map(GermanTso::name).anyMatch(tsoCode::equals);
    }
}

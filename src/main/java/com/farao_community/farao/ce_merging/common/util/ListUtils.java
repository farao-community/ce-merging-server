/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.NONE)
public final class ListUtils {

    public static <T> List<T> deNulledList(final List<T> nullable) {
        return Optional.ofNullable(nullable).orElse(new ArrayList<>());
    }

    public static <T> List<T> clonedList(final List<T> original) {
        return (original != null) ? new ArrayList<>(original) : new ArrayList<>();
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamsUtils {

    private StreamsUtils() {
        // utility class
    }

    public static <T> Stream<T> streamIterable(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}

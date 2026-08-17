/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import java.util.Collection;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamsUtils {

    private StreamsUtils() {
        // utility class
    }

    public static Double sumCollection(final Collection<Double> collection) {
        return sumProperty(collection, d -> d);
    }

    public static <T> Double sumProperty(final Collection<T> collection,
                                         final ToDoubleFunction<T> getter) {
        return sumProperty(collection.stream(), getter);
    }

    public static <T> Double sumPropertyFiltered(final Collection<T> collection,
                                                 final ToDoubleFunction<T> getter,
                                                 final Predicate<T> filter) {
        return sumPropertyFiltered(collection.stream(), getter, filter);
    }

    public static <T> Double sumProperty(final Stream<T> stream,
                                         final ToDoubleFunction<T> getter) {
        return stream
                .mapToDouble(getter)
                .sum();
    }

    public static <T> Double sumPropertyFiltered(final Stream<T> stream,
                                                 final ToDoubleFunction<T> getter,
                                                 final Predicate<T> filter) {
        return stream
                .filter(filter)
                .mapToDouble(getter)
                .sum();
    }

    public static <T> Stream<T> streamIterable(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public final class StreamsUtils {

    private StreamsUtils() {
        // utility class
    }

    /**
     * Predicate used to eliminate duplicates by property of class T
     *
     * @param getter getter is the property's getter
     * @param <T>          T any type
     * @return <T> T same type as input
     */
    public static <T> Predicate<T> distinctByProperty(final Function<? super T, ?> getter) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(getter.apply(t));
    }

    public static Double sumCollection(final Collection<Double> collection) {
        return sumProperty(collection, d -> d);
    }

    public static <T> Double sumProperty(final Collection<T> collection,
                                         final ToDoubleFunction<T> getter) {
        return collection
            .stream()
            .mapToDouble(getter)
            .sum();
    }

    public static <T> Double sumPropertyFiltered(final Collection<T> collection,
                                                 final ToDoubleFunction<T> getter,
                                                 final Predicate<T> filter) {
        return collection
            .stream()
            .filter(filter)
            .mapToDouble(getter)
            .sum();
    }
}

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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public final class StreamsUtils {

    private StreamsUtils() {
        // utility class
    }

    public static Double sum(final Collection<Double> collection) {
        return sum(collection, d -> d);
    }

    public static <T> Double sum(final Collection<? extends T> collection,
                                 final ToDoubleFunction<? super T> term) {
        return sum(collection.stream(), term);
    }

    public static <T> Double sum(final Collection<? extends T> collection,
                                 final ToDoubleFunction<? super T> term,
                                 final Predicate<? super T> filter) {
        return sum(collection.stream(), term, filter);
    }

    public static <T> Double sum(final Stream<? extends T> stream,
                                 final ToDoubleFunction<? super T> term) {
        return stream
                .mapToDouble(term)
                .sum();
    }

    public static <T> Double sum(final Stream<? extends T> stream,
                                 final ToDoubleFunction<? super T> term,
                                 final Predicate<? super T> filter) {
        return stream
                .filter(filter)
                .mapToDouble(term)
                .sum();
    }

    public static <T> Stream<T> stream(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        final Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

}

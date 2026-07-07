/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;

public class FlowByAreaMap extends HashMap<String, Double> {

    public FlowByAreaMap() {
    }

    public FlowByAreaMap(final Map<String, Double> other) {
        super(other);
    }

    public FlowByAreaMap copy() {
        return new FlowByAreaMap(this);
    }

    public void shiftFlow(final String area, final Double shift) {
        this.computeIfPresent(area, (a, np) -> np + shift);
    }

    public void shiftAllFlowsUsing(final ToDoubleFunction<String> areaOperator) {
        this.replaceAll((k, np) -> np + areaOperator.applyAsDouble(k));
    }

    public FlowByAreaMap withValuesShiftedBy(final ToDoubleFunction<String> areaOperator) {
        final FlowByAreaMap shiftedMap = new FlowByAreaMap(this);
        shiftedMap.shiftAllFlowsUsing(areaOperator);
        return shiftedMap;
    }

    public Double getOrZero(final String area) {
        return this.getOrDefault(area, 0.);
    }

    public Double getTotal() {
        return this.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public static <T> Collector<T, ?, FlowByAreaMap> toFlowByAreaMap(final Function<? super T, String> keyMapper,
                                                                     final Function<? super T, Double> valueMapper) {
        return collectingAndThen(toMap(keyMapper, valueMapper), FlowByAreaMap::new);
    }
}

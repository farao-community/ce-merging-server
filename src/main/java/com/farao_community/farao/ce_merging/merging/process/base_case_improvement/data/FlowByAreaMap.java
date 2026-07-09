/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collector;

import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumCollection;
import static java.util.function.Function.identity;
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

    /**
     *
     * @param area for which we want to update a net position
     * @param shift will be added to area's net position
     */
    public void shiftFlow(final String area, final Double shift) {
        this.computeIfPresent(area, (a, np) -> np + shift);
    }

    /**
     * Used to shift all values of this given an operator (for example, add the value from another map by area)
     * @param areaOperator operator that applies on the key (=areaId)
     */
    public void shiftAllFlowsWith(final ToDoubleFunction<String> areaOperator) {
        this.replaceAll((k, np) -> np + areaOperator.applyAsDouble(k));
    }

    /**
     * To return a copy of this map with shifted values
     * @param areaOperator operator that applies on the key (=areaId)
     */
    public FlowByAreaMap withValuesShiftedBy(final ToDoubleFunction<String> areaOperator) {
        final FlowByAreaMap shiftedMap = new FlowByAreaMap(this);
        shiftedMap.shiftAllFlowsWith(areaOperator);
        return shiftedMap;
    }

    public Double getOrZero(final String area) {
        return this.getOrDefault(area, 0.);
    }

    public Double getTotalFlow() {
        return sumCollection(values());
    }

    public static <T> Collector<T, ?, FlowByAreaMap> toFlowByAreaMap(final Function<? super T, String> keyMapper,
                                                                     final Function<? super T, Double> valueMapper) {
        return collectingAndThen(toMap(keyMapper, valueMapper), FlowByAreaMap::new);
    }

    public static FlowByAreaMap fromAreas(final Collection<String> areas, final Function<String, Double> areaToFlow) {
        return areas.stream().collect(toFlowByAreaMap(identity(), areaToFlow));
    }
}

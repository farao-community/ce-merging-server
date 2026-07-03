package com.farao_community.farao.ce_merging.base_case_improvement.process;

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

    public void shift(final String key, final Double shift) {
        this.computeIfPresent(key, (k, np) -> np + shift);
    }

    public void shiftAllBy(final ToDoubleFunction<String> shift) {
        this.replaceAll((k, np) -> np + shift.applyAsDouble(k));
    }

    public FlowByAreaMap withValuesShiftedBy(final ToDoubleFunction<String> shift) {
        final FlowByAreaMap shiftedMap = new FlowByAreaMap(this);
        shiftedMap.shiftAllBy(shift);
        return shiftedMap;
    }

    public Double getTotal() {
        return this.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public static <T, K, U> Collector<T, ?, FlowByAreaMap> toNetPositionsMap(final Function<? super T, ? extends K> keyMapper,
                                                                             final Function<? super T, ? extends U> valueMapper) {
        return collectingAndThen(toMap(keyMapper, valueMapper), FlowByAreaMap.class::cast);
    }
}

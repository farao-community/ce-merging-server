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

    public static <T, K, U> Collector<T, ?, FlowByAreaMap> toFlowByAreaMap(final Function<? super T, ? extends K> keyMapper,
                                                                           final Function<? super T, ? extends U> valueMapper) {
        return collectingAndThen(toMap(keyMapper, valueMapper), FlowByAreaMap.class::cast);
    }
}

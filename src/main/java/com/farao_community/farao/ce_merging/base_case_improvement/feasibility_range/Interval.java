/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.feasibility_range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.max;
import static java.lang.Double.min;

public class Interval {
    private double minValue;
    private double maxValue;
    private static final Logger LOGGER = LoggerFactory.getLogger(Interval.class);

    public Interval(final double minValue, final double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        if (minValue > maxValue) {
            final String errorMessage = "Min value of feasibility range should be lower than max value";
            LOGGER.error(errorMessage);
            throw new ArithmeticException(errorMessage);
        }
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(final double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(final double maxValue) {
        this.maxValue = maxValue;
    }

    public static Interval infinity() {
        return new Interval(-MAX_VALUE, MAX_VALUE);
    }

    public Interval join(final Interval interval) {
        return new Interval(max(minValue, interval.getMinValue()),
                            min(maxValue, interval.getMaxValue()));
    }

    public boolean containsValue(final Double value) {
        return value >= minValue && value <= maxValue;
    }

    public double getRange() {
        return maxValue - minValue;
    }

    public String toString() {
        return String.format("[%s, %s]", minValue, maxValue);
    }
}

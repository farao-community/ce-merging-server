/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.mockdata;

import com.farao_community.farao.ce_merging.base_case_improvement.data.inputs.Interval;

import java.util.HashMap;
import java.util.Map;

public final class FeasibilityRangeTestData {

    private FeasibilityRangeTestData() {
        throw new AssertionError("No default constructor in utility class");
    }

    public static Map<String, Interval> createFeasibilityRangeCase123() {
        Map<String, Interval> exchangeFeasibilityRanges = new HashMap<>();

        exchangeFeasibilityRanges.put("ONE", new Interval(0.0, 4000.0));
        exchangeFeasibilityRanges.put("TWELVE", new Interval(-2500.0, -500.0));
        exchangeFeasibilityRanges.put("EIGHT", new Interval(-700.0, 1300));
        exchangeFeasibilityRanges.put("TWENTYFIVE", new Interval(-1000.0, 0.0));

        return exchangeFeasibilityRanges;
    }

    public static Map<String, Interval> createFeasibilityRangeCase4() {
        Map<String, Interval> exchangeFeasibilityRanges = new HashMap<>();

        exchangeFeasibilityRanges.put("ONE", new Interval(0.0, 4000.0));
        exchangeFeasibilityRanges.put("TWELVE", new Interval(-2500.0, -500.0));
        exchangeFeasibilityRanges.put("EIGHT", new Interval(700.0, 2700.0));
        exchangeFeasibilityRanges.put("TWENTYFIVE", new Interval(300.0, 1300.0));

        return exchangeFeasibilityRanges;
    }

    public static Map<String, Interval> createFeasibilityRangeCase5() {
        Map<String, Interval> exchangeFeasibilityRanges = new HashMap<>();

        exchangeFeasibilityRanges.put("ONE", new Interval(0.0, 4000.0));
        exchangeFeasibilityRanges.put("TWELVE", new Interval(-3200.0, -1200.0));
        exchangeFeasibilityRanges.put("EIGHT", new Interval(1300.0, 3300.0));
        exchangeFeasibilityRanges.put("TWENTYFIVE", new Interval(3600.0, 4600.0));

        return exchangeFeasibilityRanges;
    }

    public static Map<String, Interval> createFeasibilityRangeCase6() {
        Map<String, Interval> exchangeFeasibilityRanges = new HashMap<>();

        exchangeFeasibilityRanges.put("ONE", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("TWELVE", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("EIGHT", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("TWENTYFIVE", new Interval(-1000.0, 1000.0));

        return exchangeFeasibilityRanges;
    }

    public static Map<String, Interval> createFeasibilityRangeCase7() {
        Map<String, Interval> exchangeFeasibilityRanges = new HashMap<>();

        exchangeFeasibilityRanges.put("ONE", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("TWELVE", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("EIGHT", new Interval(-200.0, 200.0));
        exchangeFeasibilityRanges.put("TWENTYFIVE", new Interval(-1400.0, -1200.0));

        return exchangeFeasibilityRanges;
    }
}

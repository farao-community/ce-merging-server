/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package test_utils.assertions;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.LoadingLimits;
import com.powsybl.iidm.network.OperationalLimitsGroup;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LineAssert extends AbstractAssert<LineAssert, Line> {
    protected LineAssert(final Line line) {
        super(line, LineAssert.class);
    }

    public static LineAssert assertThat(final Line line) {
        return new LineAssert(line);
    }

    @CanIgnoreReturnValue
    public LineAssert hasProperties(final Map<String, String> properties) {
        if (actual == null) {
            failWithMessage("line doesn't exist");
        } else {
            properties.forEach((key, expectedValue) -> {
                final String linePropValue = actual.getProperty(key);
                if (linePropValue == null) {
                    failWithMessage("line %s doesn't have property %s".formatted(actual.getId(), key));
                } else if (!linePropValue.equals(expectedValue)) {
                    failWithMessage("line %s has %s:%s, but expected %s".formatted(actual.getId(), key, linePropValue, expectedValue));
                }
            });
        }
        return this;
    }

    @CanIgnoreReturnValue
    public LineAssert hasPermanentCurrentLimits(final Map<String, Double> limits) {
        return hasLimits(limits, OperationalLimitsGroup::getCurrentLimits, null);
    }

    @CanIgnoreReturnValue
    public LineAssert hasLimits(final Map<String, Double> limits,
                                final Function<OperationalLimitsGroup, Optional<? extends LoadingLimits>> limitGetter,
                                final Integer acceptableDuration) {
        if (actual == null) {
            failWithMessage("line doesn't exist");
            return this;
        }

        final String allLimits = Stream.concat(actual.getOperationalLimitsGroups1().stream(),
                                               actual.getOperationalLimitsGroups2().stream())
                .map(OperationalLimitsGroup::getId).collect(Collectors.joining(", "));

        limits.forEach((key, value) -> {
            final Optional<OperationalLimitsGroup> group1 = actual.getOperationalLimitsGroup1(key);
            final Optional<OperationalLimitsGroup> group2 = actual.getOperationalLimitsGroup2(key);
            if (group1.isEmpty() && group2.isEmpty()) {
                failWithMessage("line %s doesn't have limit %s (possible values : %s)",
                                actual.getId(), key, allLimits);
            } else {
                final Optional<? extends LoadingLimits> limits1 = group1.flatMap(limitGetter::apply);
                final Optional<? extends LoadingLimits> limits2 = group2.flatMap(limitGetter::apply);

                Stream.of(limits1, limits2).forEach(loadingLimits -> {
                    if (loadingLimits.isPresent()) {
                        final double actualLimit = getApplicableLimit(loadingLimits.get(), acceptableDuration);
                        if (actualLimit != value) {
                            failWithMessage("line %s : expected limit %f, but was %f",
                                            actual.getId(), value, actualLimit);
                        }
                    }
                });
            }

        });

        return this;
    }

    private double getApplicableLimit(final LoadingLimits limits,
                                      final Integer acceptableDuration) {
        return Optional.ofNullable(acceptableDuration)
                .map(limits::getTemporaryLimit)
                .map(LoadingLimits.TemporaryLimit::getValue)
                .orElse(limits.getPermanentLimit());
    }

}

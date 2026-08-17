/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package test_utils.assertions;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BciAreaResultsAssert extends AbstractAssert<BciAreaResultsAssert, BciAreaResults> {
    private static final double EPSILON = 0.01;

    protected BciAreaResultsAssert(final BciAreaResults bciAreaResults) {
        super(bciAreaResults, BciAreaResultsAssert.class);
    }

    public static BciAreaResultsAssert assertThat(final BciAreaResults bciAreaResults) {
        return new BciAreaResultsAssert(bciAreaResults);
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInNpf(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().forecast(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasForecast(final double expected) {
        return hasGlobalNpf(expected).hasInNpf(expected);
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasTarget(final double expected) {
        return hasGlobalTargetNp(expected).hasInTargetNp(expected);
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasGlobalNpf(final double expected) {
        if (actual.getGlobalNetPositions() == null) {
            failWithMessage("No global net positions found");
        }
        assertEquals(expected, actual.getGlobalNetPositions().forecast(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInTargetNp(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().target(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasGlobalTargetNp(final double expected) {
        if (actual.getGlobalNetPositions() == null) {
            failWithMessage("No global net positions found");
        }
        assertEquals(expected, actual.getGlobalNetPositions().target(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hadBciApplied(final boolean bciApplied) {
        assertEquals(bciApplied, actual.getBciApplied());
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInInitialMin(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().initialMin(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInInitialMax(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().initialMax(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInFinalMin(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().finalMin(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInFinalMax(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().finalMax(), EPSILON);
        return this;
    }

}

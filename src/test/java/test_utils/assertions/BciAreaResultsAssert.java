package test_utils.assertions;

import com.farao_community.farao.ce_merging.base_case_improvement.data.result.BciAreaResults;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(expected, actual.getInRegionNetPositions().getForecast(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasGlobalNpf(final double expected) {
        if (actual.getGlobalNetPositions() == null) {
            failWithMessage("No global net positions found");
        }
        assertEquals(expected, actual.getGlobalNetPositions().getForecast(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInTargetNp(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().getTarget(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasGlobalTargetNp(final double expected) {
        if (actual.getGlobalNetPositions() == null) {
            failWithMessage("No global net positions found");
        }
        assertEquals(expected, actual.getGlobalNetPositions().getTarget(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hadBciApplied() {
        assertTrue(actual.getBciApplied());
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hadNoBciApplied() {
        assertFalse(actual.getBciApplied());
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInInitialMin(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().getInitialMin(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInInitialMax(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().getInitialMax(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInFinalMin(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().getFinalMin(), EPSILON);
        return this;
    }

    @CanIgnoreReturnValue
    public BciAreaResultsAssert hasInFinalMax(final double expected) {
        if (actual.getInRegionNetPositions() == null) {
            failWithMessage("No inRegion net positions found");
        }
        assertEquals(expected, actual.getInRegionNetPositions().getFinalMax(), EPSILON);
        return this;
    }

}

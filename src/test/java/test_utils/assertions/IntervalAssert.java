package test_utils.assertions;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntervalAssert extends AbstractAssert<IntervalAssert, Interval> {
    private static final double EPSILON = 0.01;

    protected IntervalAssert(final Interval interval) {
        super(interval, IntervalAssert.class);
    }

    public static IntervalAssert assertThat(final Interval interval) {
        return new IntervalAssert(interval);
    }

    @CanIgnoreReturnValue
    public IntervalAssert rangeIs(final double min, final double max) {
        assertEquals(min, actual.getMinValue(), EPSILON);
        assertEquals(max, actual.getMaxValue(), EPSILON);
        return this;
    }
}

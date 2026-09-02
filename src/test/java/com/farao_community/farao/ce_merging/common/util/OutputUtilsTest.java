package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutputUtilsTest {

    @ParameterizedTest
    @MethodSource("calculateTargetPositionCases")
    void calculateTargetPositionShouldReturnExpectedPosition(final String targetDate, final int expectedPosition) {
        final OffsetDateTime periodStart = OffsetDateTime.parse("2020-01-06T22:00Z");
        final OffsetDateTime periodEnd = OffsetDateTime.parse("2020-01-07T23:00Z");
        final int position = OutputUtils.calculateTargetPosition(OffsetDateTime.parse(targetDate), periodStart, periodEnd);
        assertEquals(expectedPosition, position);
    }

    private static Stream<Arguments> calculateTargetPositionCases() {
        return Stream.of(
                Arguments.of("2020-01-06T22:00Z", 1),
                Arguments.of("2020-01-06T23:33Z", 2),
                Arguments.of("2020-01-07T21:33Z", 24),
                Arguments.of("2020-01-07T22:33Z", 25)
        );
    }

    @Test
    void calculateTargetPositionShouldThrowExceptionWhenTargetIsAfterPeriodEnd() {
        final OffsetDateTime targetDate = OffsetDateTime.parse("2020-01-07T21:33Z");
        final OffsetDateTime periodStart = OffsetDateTime.parse("2020-01-06T22:00Z");
        final OffsetDateTime periodEnd = OffsetDateTime.parse("2020-01-07T21:00Z");
        assertThrows(
                CeMergingException.class,
                () -> OutputUtils.calculateTargetPosition(
                        targetDate,
                        periodStart,
                        periodEnd
                )
        );
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package test_utils.assertions;

import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.CanIgnoreReturnValue;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TimeSeriesAssert extends AbstractAssert<TimeSeriesAssert, PublicationDocument.PublicationTimeSeries> {

    protected TimeSeriesAssert(final PublicationDocument.PublicationTimeSeries timeSeries) {
        super(timeSeries, TimeSeriesAssert.class);
    }

    public static TimeSeriesAssert assertThat(final PublicationDocument.PublicationTimeSeries timeSeries) {
        return new TimeSeriesAssert(timeSeries);
    }

    @CanIgnoreReturnValue
    public TimeSeriesAssert isIdentifiedBy(final String id) {
        assertEquals(id, actual.getTimeSeriesIdentification().getV());
        return this;
    }

    @CanIgnoreReturnValue
    public TimeSeriesAssert links(final String outArea,
                                  final String inArea) {
        assertEquals(outArea, actual.getOutArea().getV());
        assertEquals(inArea, actual.getInArea().getV());
        return this;
    }

    @CanIgnoreReturnValue
    public TimeSeriesAssert hasPoint(final int position,
                                     final int quantity) {
        final PublicationDocument.PublicationTimeSeries.Period.Interval atThisPosition = actual.getPeriod().getInterval().get(position - 1);
        assertEquals(position, atThisPosition.getPos().getV().intValue());
        assertEquals(quantity, atThisPosition.getQty().getV().intValue());
        return this;
    }

    @CanIgnoreReturnValue
    public TimeSeriesAssert isFullDay() {
        assertEquals(24, actual.getPeriod().getInterval().size());
        return this;
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.BusinessType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class GlskSerieRedispatcherTest {

    public static final String AREA_1 = "AREA1";
    public static final String TS_001 = "TS001";
    public static final String TS_002 = "TS002";

    @Test
    void shouldStoreValueByArea() {
        final Map<String, List<GlskRedispatchingEntity>> values = new HashMap<>();

        GlskSerieRedispatcher.storeValue(
                values,
                AREA_1,
                TS_001,
                20.0
        );

        assertTrue(values.containsKey(AREA_1));
        assertEquals(1, values.get(AREA_1).size());
        assertEquals(TS_001, values.get(AREA_1).getFirst().getId());
        assertEquals(20.0, values.get(AREA_1).getFirst().getShare());
    }

    @Test
    void shouldRedispatchShareWhenSumIsBelow100() {
        final GSKSeriesType series1 = createGskSeries(TS_001);
        final GSKSeriesType series2 = createGskSeries(TS_002);
        final GSKDocument document = new GSKDocument();
        document.getGSKSeries().add(series1);
        document.getGSKSeries().add(series2);

        final Map<String, List<GlskRedispatchingEntity>> values = new HashMap<>();
        values.put(AREA_1, List.of(
                        new GlskRedispatchingEntity(TS_001, 50.0),
                        new GlskRedispatchingEntity(TS_002, 25.0)
                )
        );

        GlskSerieRedispatcher.redispatchShareValue(values, document);

        /*
         * 50 + 25 = 75
         * adjustment = 25
         *
         * TS001:
         * 50 + (50/75)*25 = 66.66 => 67
         *
         * TS002:
         * 25 + (25/75)*25 = 33.33 => 33
         */
        assertEquals(BigDecimal.valueOf(67), series1.getBusinessType().getShare());
        assertEquals(BigDecimal.valueOf(33), series2.getBusinessType().getShare()
        );
    }

    @Test
    void shouldNotRedispatchWhenShareSumIs100() {
        final GSKSeriesType series1 = createGskSeries(TS_001);
        final GSKSeriesType series2 = createGskSeries(TS_002);
        final GSKDocument document = new GSKDocument();
        document.getGSKSeries().add(series1);
        document.getGSKSeries().add(series2);

        series1.getBusinessType().setShare(BigDecimal.valueOf(50));
        series2.getBusinessType().setShare(BigDecimal.valueOf(70));

        final Map<String, List<GlskRedispatchingEntity>> values = new HashMap<>();
        values.put(AREA_1, List.of(
                new GlskRedispatchingEntity(TS_001, 50.0),
                new GlskRedispatchingEntity(TS_002, 70.0)
        ));

        GlskSerieRedispatcher.redispatchShareValue(values, document);

        assertEquals(BigDecimal.valueOf(50), series1.getBusinessType().getShare());
        assertEquals(BigDecimal.valueOf(70), series2.getBusinessType().getShare());
    }

    @Test
    void shouldThrowExceptionWhenShareSumIsZero() {
        final GSKSeriesType series = createGskSeries(TS_001);

        final GSKDocument document = new GSKDocument();
        document.getGSKSeries().add(series);

        final Map<String, List<GlskRedispatchingEntity>> values = new HashMap<>();
        values.put(AREA_1, List.of(new GlskRedispatchingEntity(TS_001, 0.0)));

        assertThrows(
                CeMergingException.class,
                () -> GlskSerieRedispatcher.redispatchShareValue(values, document)
        );
    }

    private GSKSeriesType createGskSeries(String id) {
        final GSKSeriesType series = new GSKSeriesType();
        final IdentificationType identification = new IdentificationType();
        identification.setV(id);
        series.setTimeSeriesIdentification(identification);
        BusinessType businessType = new BusinessType();
        series.setBusinessType(businessType);
        return series;
    }
}

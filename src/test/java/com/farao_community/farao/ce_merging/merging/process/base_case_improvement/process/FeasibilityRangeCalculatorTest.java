/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraints;
import static java.lang.Double.MAX_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;
import static test_utils.assertions.IntervalAssert.assertThat;

class FeasibilityRangeCalculatorTest {
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2019-03-01T23:00Z");
    private final FlowByAreaMap netPositionsMap = new FlowByAreaMap();
    private byte[] externalConstraints;
    private byte[] bciFeasibilityRange;
    private static final byte[] EMPTY_FEASIBILITY_RANGE = new byte[0];
    private static final FlowByAreaMap EMPTY_NETPOSITIONS_MAP = new FlowByAreaMap();
    private RegionConfiguration regionConfiguration;
    private FeasibilityRangeCalculator feasibilityRangeCalculator;

    @BeforeEach
    void setUp() throws IOException {
        netPositionsMap.put("AREA_NUMBER11_EIC", 100.0);
        netPositionsMap.put("AREA_NUMBER2_EIC", 200.0);
        netPositionsMap.put("AREA_NUMBER8_EIC", 300.0);
        netPositionsMap.put("AREA_NUMBER13_EIC", 400.0);
        netPositionsMap.put("AREA_NUMBER12_EIC", 500.0);
        netPositionsMap.put("AREA_NUMBER10_EIC", 100.0);
        netPositionsMap.put("AREA_NUMBER3_EIC", 200.0);
        netPositionsMap.put("AREA_NUMBER1_EIC", 300.0);
        netPositionsMap.put("AREA_NUMBER5_EIC", 400.0);
        netPositionsMap.put("AREA_NUMBER23_EIC", 500.0);
        netPositionsMap.put("AREA_NUMBER25_EIC", 600.0);

        final String jsonConfig = new String(getBciTestFile("region_configuration.json"));
        regionConfiguration = new ObjectMapper().readValue(jsonConfig, RegionConfiguration.class);
        feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
    }

    @Test
    void calculateExternalConstraintsTest() throws IOException {
        externalConstraints = getBciTestFile("externalConstraints.xml");

        final Map<String, Interval> ecs = calculateConstraints(externalConstraints, regionConfiguration, TARGET_DATE);

        assertEquals(12, ecs.size());

        assertThat(ecs.get("AREA_NUMBER12_EIC")).rangeIs(0, MAX_VALUE);
        assertThat(ecs.get("AREA_NUMBER25_EIC")).rangeIs(0, 11285);
        assertThat(ecs.get("AREA_NUMBER26_EIC")).rangeIs(0, 11285);
        assertThat(ecs.get("AREA_NUMBER2_EIC")).rangeIs(-MAX_VALUE, MAX_VALUE);

        Map<String, Interval> frs = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints,
                                                                                          TARGET_DATE,
                                                                                          EMPTY_NETPOSITIONS_MAP,
                                                                                          EMPTY_FEASIBILITY_RANGE);
        assertEquals(12, frs.size());

        assertThat(frs.get("AREA_NUMBER12_EIC")).rangeIs(0, MAX_VALUE);
        assertThat(frs.get("AREA_NUMBER25_EIC")).rangeIs(0, 11285);
        assertThat(frs.get("AREA_NUMBER26_EIC")).rangeIs(0, 11285);
        assertThat(frs.get("AREA_NUMBER2_EIC")).rangeIs(-MAX_VALUE, MAX_VALUE);

    }

    @Test
    void calculateFeasibilityRangeWithNetPosition() throws IOException {
        bciFeasibilityRange = getBciTestFile("bciFeasibilityRange.xml");
        final Map<String, Interval> frs = feasibilityRangeCalculator.importFeasibilityRangesFile(bciFeasibilityRange, netPositionsMap);

        assertEquals(12, frs.size());

        assertThat(frs.get("AREA_NUMBER11_EIC")).rangeIs(-900, 1100);
        assertThat(frs.get("AREA_NUMBER2_EIC")).rangeIs(0, 400);
        assertThat(frs.get("AREA_NUMBER8_EIC")).rangeIs(-4700, 5300);
        assertThat(frs.get("AREA_NUMBER13_EIC")).rangeIs(100, 700);
        assertThat(frs.get("AREA_NUMBER12_EIC")).rangeIs(-1500, 2500);
        assertThat(frs.get("AREA_NUMBER10_EIC")).rangeIs(-50, 250);
        assertThat(frs.get("AREA_NUMBER3_EIC")).rangeIs(-300, 700);
        assertThat(frs.get("AREA_NUMBER26_EIC")).rangeIs(-300, 1000);
        assertThat(frs.get("AREA_NUMBER1_EIC")).rangeIs(-3700, 3300);
        assertThat(frs.get("AREA_NUMBER5_EIC")).rangeIs(300, 500);
        assertThat(frs.get("AREA_NUMBER23_EIC")).rangeIs(300, 700);
        assertThat(frs.get("AREA_NUMBER25_EIC")).rangeIs(-900, 2100);
    }

    @Test
    void calculateFeasibilityRangeWithoutNetPosition() throws IOException {
        bciFeasibilityRange = getBciTestFile("bciFeasibilityRange.xml");

        assertThatThrownBy(() -> feasibilityRangeCalculator.importFeasibilityRangesFile(bciFeasibilityRange,
                                                                                        EMPTY_NETPOSITIONS_MAP))
            .isValidServiceException();
    }

    @Test
    void getRegionFeasibilityRangesTest() throws IOException {
        externalConstraints = getBciTestFile("externalConstraints.xml");
        bciFeasibilityRange = getBciTestFile("bciFeasibilityRange.xml");
        Map<String, Interval> frs = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints,
                                                                                          TARGET_DATE,
                                                                                          netPositionsMap,
                                                                                          bciFeasibilityRange);

        assertThat(frs.get("AREA_NUMBER11_EIC")).rangeIs(-900, 1100);
        assertThat(frs.get("AREA_NUMBER2_EIC")).rangeIs(0, 400);
        assertThat(frs.get("AREA_NUMBER8_EIC")).rangeIs(-4700, 5300);
        assertThat(frs.get("AREA_NUMBER13_EIC")).rangeIs(100, 700);
        assertThat(frs.get("AREA_NUMBER12_EIC")).rangeIs(0, 2500);
        assertThat(frs.get("AREA_NUMBER10_EIC")).rangeIs(-50, 250);
        assertThat(frs.get("AREA_NUMBER3_EIC")).rangeIs(-300, 700);
        assertThat(frs.get("AREA_NUMBER26_EIC")).rangeIs(0, 1000);
        assertThat(frs.get("AREA_NUMBER1_EIC")).rangeIs(-3700, 3300);
        assertThat(frs.get("AREA_NUMBER5_EIC")).rangeIs(300, 500);
        assertThat(frs.get("AREA_NUMBER23_EIC")).rangeIs(300, 700);
        assertThat(frs.get("AREA_NUMBER25_EIC")).rangeIs(0, 2100);
    }

    private byte[] getBciTestFile(final String name) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream("/bci/%s".formatted(name)));
    }
}

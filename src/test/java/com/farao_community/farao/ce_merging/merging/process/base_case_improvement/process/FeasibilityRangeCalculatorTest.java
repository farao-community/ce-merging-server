/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.CeTestUtils;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.ExternalConstraintsImporter.calculateConstraints;
import static java.lang.Double.MAX_VALUE;
import static java.nio.file.Files.readAllBytes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static test_utils.assertions.IntervalAssert.assertThat;

@SpringBootTest
class FeasibilityRangeCalculatorTest {
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2019-03-01T23:00Z");
    private final Map<String, Double> netPositionsMap = new HashMap<>();
    private byte[] externalConstraints;
    private byte[] bciFeasibilityRange;
    private static final byte[] EMPTY_FEASIBILITY_RANGE = new byte[0];
    private static final Map<String, Double> EMPTY_NETPOSITIONS_MAP = new HashMap<>();
    private RegionConfiguration regionConfiguration;

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

        final ObjectMapper objectMapper = new ObjectMapper();
        final File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        final String jsonConfig = new String(readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);
    }

    @Test
    void calculateExternalConstraintsTest() throws IOException {
        externalConstraints = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/externalConstraints.xml"));
        final FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
        final Map<String, Interval> ecs = calculateConstraints(externalConstraints, regionConfiguration, TARGET_DATE);

        assertEquals(12, ecs.size());

        assertThat(ecs.get("AREA_NUMBER12_EIC")).rangeIs(0, MAX_VALUE);
        assertThat(ecs.get("AREA_NUMBER25_EIC")).rangeIs(0, 11285);
        assertThat(ecs.get("AREA_NUMBER26_EIC")).rangeIs(0, 11285);
        assertThat(ecs.get("AREA_NUMBER2_EIC")).rangeIs(-MAX_VALUE, MAX_VALUE);

        Map<String, Interval> frs = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, EMPTY_NETPOSITIONS_MAP, EMPTY_FEASIBILITY_RANGE);
        assertEquals(12, frs.size());

        assertThat(frs.get("AREA_NUMBER12_EIC")).rangeIs(0, MAX_VALUE);
        assertThat(frs.get("AREA_NUMBER25_EIC")).rangeIs(0, 11285);
        assertThat(frs.get("AREA_NUMBER26_EIC")).rangeIs(0, 11285);
        assertThat(frs.get("AREA_NUMBER2_EIC")).rangeIs(-MAX_VALUE, MAX_VALUE);

    }

    @Test
    void calculateFeasibilityRangeWithNetPosition() throws JAXBException, IOException {
        bciFeasibilityRange = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
        final FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
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
    void calculateFeasibilityRangeWithoutNetPosition() throws JAXBException, IOException {
        try {
            bciFeasibilityRange = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
            FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
            feasibilityRangeCalculator.importFeasibilityRangesFile(bciFeasibilityRange, EMPTY_NETPOSITIONS_MAP);
            fail();
        } catch (CeMergingException e) {
            //should throw exception
        }
    }

    @Test
    void getRegionFeasibilityRangesTest() throws IOException {
        externalConstraints = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/externalConstraints.xml"));
        bciFeasibilityRange = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
        FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
        Map<String, Interval> frs = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, netPositionsMap, bciFeasibilityRange);

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
}

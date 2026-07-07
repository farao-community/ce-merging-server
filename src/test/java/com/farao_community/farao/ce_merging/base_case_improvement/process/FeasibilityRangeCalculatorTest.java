/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.base_case_improvement.process;

import com.farao_community.farao.ce_merging.base_case_improvement.data.inputs.Interval;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class FeasibilityRangeCalculatorTest {
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2019-03-01T23:00Z");
    private Map<String, Double> netPositionsMap = new HashMap<>();
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

        ObjectMapper objectMapper = new ObjectMapper();
        File resourceRegion = CeTestUtils.pathOf("bci/region_configuration.json").toFile();
        String jsonConfig = new String(java.nio.file.Files.readAllBytes(resourceRegion.toPath()));
        regionConfiguration = objectMapper.readValue(jsonConfig, RegionConfiguration.class);
    }

    @Test
    void calculateExternalConstraintsTest() throws IOException {
        externalConstraints = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/externalConstraints.xml"));
        FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
        Map<String, Interval> externalConstraintResult = ExternalConstraintsImporter.calculateConstraints(externalConstraints, regionConfiguration, TARGET_DATE);

        assertEquals(12, externalConstraintResult.entrySet().size());
        assertEquals(0, externalConstraintResult.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, externalConstraintResult.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(0, externalConstraintResult.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(11285, externalConstraintResult.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);

        assertEquals(0, externalConstraintResult.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(11285, externalConstraintResult.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-Double.MAX_VALUE, externalConstraintResult.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, externalConstraintResult.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        Map<String, Interval> regionFeasibilityRangeResult = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, EMPTY_NETPOSITIONS_MAP, EMPTY_FEASIBILITY_RANGE);
        assertEquals(12, regionFeasibilityRangeResult.entrySet().size());
        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, regionFeasibilityRangeResult.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(11285, regionFeasibilityRangeResult.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(11285, regionFeasibilityRangeResult.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-Double.MAX_VALUE, regionFeasibilityRangeResult.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, regionFeasibilityRangeResult.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);
    }

    @Test
    void calculateFeasibilityRangeWithNetPosition() throws JAXBException, IOException {
        bciFeasibilityRange = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
        FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
        Map<String, Interval> feasibilityRangeResult = feasibilityRangeCalculator.importFeasibilityRangesFile(bciFeasibilityRange, netPositionsMap);

        assertEquals(12, feasibilityRangeResult.entrySet().size());
        assertEquals(-900, feasibilityRangeResult.get("AREA_NUMBER11_EIC").getMinValue(), 1e-3);
        assertEquals(1100, feasibilityRangeResult.get("AREA_NUMBER11_EIC").getMaxValue(), 1e-3);

        assertEquals(0, feasibilityRangeResult.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(400, feasibilityRangeResult.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        assertEquals(-4700, feasibilityRangeResult.get("AREA_NUMBER8_EIC").getMinValue(), 1e-3);
        assertEquals(5300, feasibilityRangeResult.get("AREA_NUMBER8_EIC").getMaxValue(), 1e-3);

        assertEquals(100, feasibilityRangeResult.get("AREA_NUMBER13_EIC").getMinValue(), 1e-3);
        assertEquals(700, feasibilityRangeResult.get("AREA_NUMBER13_EIC").getMaxValue(), 1e-3);

        assertEquals(-1500, feasibilityRangeResult.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(2500, feasibilityRangeResult.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(-50, feasibilityRangeResult.get("AREA_NUMBER10_EIC").getMinValue(), 1e-3);
        assertEquals(250, feasibilityRangeResult.get("AREA_NUMBER10_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, feasibilityRangeResult.get("AREA_NUMBER3_EIC").getMinValue(), 1e-3);
        assertEquals(700, feasibilityRangeResult.get("AREA_NUMBER3_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, feasibilityRangeResult.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(1000, feasibilityRangeResult.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-3700, feasibilityRangeResult.get("AREA_NUMBER1_EIC").getMinValue(), 1e-3);
        assertEquals(3300, feasibilityRangeResult.get("AREA_NUMBER1_EIC").getMaxValue(), 1e-3);

        assertEquals(300, feasibilityRangeResult.get("AREA_NUMBER5_EIC").getMinValue(), 1e-3);
        assertEquals(500, feasibilityRangeResult.get("AREA_NUMBER5_EIC").getMaxValue(), 1e-3);

        assertEquals(300, feasibilityRangeResult.get("AREA_NUMBER23_EIC").getMinValue(), 1e-3);
        assertEquals(700, feasibilityRangeResult.get("AREA_NUMBER23_EIC").getMaxValue(), 1e-3);

        assertEquals(-900, feasibilityRangeResult.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(2100, feasibilityRangeResult.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);
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
        Map<String, Interval> regionFeasibilityRangeResult = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, netPositionsMap, bciFeasibilityRange);

        assertEquals(-900, regionFeasibilityRangeResult.get("AREA_NUMBER11_EIC").getMinValue(), 1e-3);
        assertEquals(1100, regionFeasibilityRangeResult.get("AREA_NUMBER11_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(400, regionFeasibilityRangeResult.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        assertEquals(-4700, regionFeasibilityRangeResult.get("AREA_NUMBER8_EIC").getMinValue(), 1e-3);
        assertEquals(5300, regionFeasibilityRangeResult.get("AREA_NUMBER8_EIC").getMaxValue(), 1e-3);

        assertEquals(100, regionFeasibilityRangeResult.get("AREA_NUMBER13_EIC").getMinValue(), 1e-3);
        assertEquals(700, regionFeasibilityRangeResult.get("AREA_NUMBER13_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(2500, regionFeasibilityRangeResult.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(-50, regionFeasibilityRangeResult.get("AREA_NUMBER10_EIC").getMinValue(), 1e-3);
        assertEquals(250, regionFeasibilityRangeResult.get("AREA_NUMBER10_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, regionFeasibilityRangeResult.get("AREA_NUMBER3_EIC").getMinValue(), 1e-3);
        assertEquals(700, regionFeasibilityRangeResult.get("AREA_NUMBER3_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(1000, regionFeasibilityRangeResult.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-3700, regionFeasibilityRangeResult.get("AREA_NUMBER1_EIC").getMinValue(), 1e-3);
        assertEquals(3300, regionFeasibilityRangeResult.get("AREA_NUMBER1_EIC").getMaxValue(), 1e-3);

        assertEquals(300, regionFeasibilityRangeResult.get("AREA_NUMBER5_EIC").getMinValue(), 1e-3);
        assertEquals(500, regionFeasibilityRangeResult.get("AREA_NUMBER5_EIC").getMaxValue(), 1e-3);

        assertEquals(300, regionFeasibilityRangeResult.get("AREA_NUMBER23_EIC").getMinValue(), 1e-3);
        assertEquals(700, regionFeasibilityRangeResult.get("AREA_NUMBER23_EIC").getMaxValue(), 1e-3);

        assertEquals(0, regionFeasibilityRangeResult.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(2100, regionFeasibilityRangeResult.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);
    }
}

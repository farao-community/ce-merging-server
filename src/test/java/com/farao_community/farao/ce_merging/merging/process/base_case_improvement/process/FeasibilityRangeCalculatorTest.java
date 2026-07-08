/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.Interval;
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
        Map<String, Interval> ecs = ExternalConstraintsImporter.calculateConstraints(externalConstraints, regionConfiguration, TARGET_DATE);

        assertEquals(12, ecs.size());
        assertEquals(0, ecs.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, ecs.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(0, ecs.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(11285, ecs.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);

        assertEquals(0, ecs.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(11285, ecs.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-Double.MAX_VALUE, ecs.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, ecs.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        Map<String, Interval> frs = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, EMPTY_NETPOSITIONS_MAP, EMPTY_FEASIBILITY_RANGE);
        assertEquals(12, frs.size());
        assertEquals(0, frs.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, frs.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(0, frs.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(11285, frs.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);

        assertEquals(0, frs.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(11285, frs.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-Double.MAX_VALUE, frs.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(Double.MAX_VALUE, frs.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);
    }

    @Test
    void calculateFeasibilityRangeWithNetPosition() throws JAXBException, IOException {
        bciFeasibilityRange = IOUtils.toByteArray(getClass().getResourceAsStream("/bci/bciFeasibilityRange.xml"));
        FeasibilityRangeCalculator feasibilityRangeCalculator = new FeasibilityRangeCalculator(regionConfiguration);
        Map<String, Interval> fr = feasibilityRangeCalculator.importFeasibilityRangesFile(bciFeasibilityRange, netPositionsMap);

        assertEquals(12, fr.entrySet().size());
        assertEquals(-900, fr.get("AREA_NUMBER11_EIC").getMinValue(), 1e-3);
        assertEquals(1100, fr.get("AREA_NUMBER11_EIC").getMaxValue(), 1e-3);

        assertEquals(0, fr.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(400, fr.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        assertEquals(-4700, fr.get("AREA_NUMBER8_EIC").getMinValue(), 1e-3);
        assertEquals(5300, fr.get("AREA_NUMBER8_EIC").getMaxValue(), 1e-3);

        assertEquals(100, fr.get("AREA_NUMBER13_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER13_EIC").getMaxValue(), 1e-3);

        assertEquals(-1500, fr.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(2500, fr.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(-50, fr.get("AREA_NUMBER10_EIC").getMinValue(), 1e-3);
        assertEquals(250, fr.get("AREA_NUMBER10_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, fr.get("AREA_NUMBER3_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER3_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, fr.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(1000, fr.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-3700, fr.get("AREA_NUMBER1_EIC").getMinValue(), 1e-3);
        assertEquals(3300, fr.get("AREA_NUMBER1_EIC").getMaxValue(), 1e-3);

        assertEquals(300, fr.get("AREA_NUMBER5_EIC").getMinValue(), 1e-3);
        assertEquals(500, fr.get("AREA_NUMBER5_EIC").getMaxValue(), 1e-3);

        assertEquals(300, fr.get("AREA_NUMBER23_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER23_EIC").getMaxValue(), 1e-3);

        assertEquals(-900, fr.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(2100, fr.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);
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
        Map<String, Interval> fr = feasibilityRangeCalculator.getRegionFeasibilityRanges(externalConstraints, TARGET_DATE, netPositionsMap, bciFeasibilityRange);

        assertEquals(-900, fr.get("AREA_NUMBER11_EIC").getMinValue(), 1e-3);
        assertEquals(1100, fr.get("AREA_NUMBER11_EIC").getMaxValue(), 1e-3);

        assertEquals(0, fr.get("AREA_NUMBER2_EIC").getMinValue(), 1e-3);
        assertEquals(400, fr.get("AREA_NUMBER2_EIC").getMaxValue(), 1e-3);

        assertEquals(-4700, fr.get("AREA_NUMBER8_EIC").getMinValue(), 1e-3);
        assertEquals(5300, fr.get("AREA_NUMBER8_EIC").getMaxValue(), 1e-3);

        assertEquals(100, fr.get("AREA_NUMBER13_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER13_EIC").getMaxValue(), 1e-3);

        assertEquals(0, fr.get("AREA_NUMBER12_EIC").getMinValue(), 1e-3);
        assertEquals(2500, fr.get("AREA_NUMBER12_EIC").getMaxValue(), 1e-3);

        assertEquals(-50, fr.get("AREA_NUMBER10_EIC").getMinValue(), 1e-3);
        assertEquals(250, fr.get("AREA_NUMBER10_EIC").getMaxValue(), 1e-3);

        assertEquals(-300, fr.get("AREA_NUMBER3_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER3_EIC").getMaxValue(), 1e-3);

        assertEquals(0, fr.get("AREA_NUMBER26_EIC").getMinValue(), 1e-3);
        assertEquals(1000, fr.get("AREA_NUMBER26_EIC").getMaxValue(), 1e-3);

        assertEquals(-3700, fr.get("AREA_NUMBER1_EIC").getMinValue(), 1e-3);
        assertEquals(3300, fr.get("AREA_NUMBER1_EIC").getMaxValue(), 1e-3);

        assertEquals(300, fr.get("AREA_NUMBER5_EIC").getMinValue(), 1e-3);
        assertEquals(500, fr.get("AREA_NUMBER5_EIC").getMaxValue(), 1e-3);

        assertEquals(300, fr.get("AREA_NUMBER23_EIC").getMinValue(), 1e-3);
        assertEquals(700, fr.get("AREA_NUMBER23_EIC").getMaxValue(), 1e-3);

        assertEquals(0, fr.get("AREA_NUMBER25_EIC").getMinValue(), 1e-3);
        assertEquals(2100, fr.get("AREA_NUMBER25_EIC").getMaxValue(), 1e-3);
    }
}

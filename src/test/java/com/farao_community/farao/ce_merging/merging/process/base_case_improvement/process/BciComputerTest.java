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
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.InRegionNetPositions;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process.BciComputerTest.ExpectedBciResults.values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.assertions.BciAreaResultsAssert.assertThat;
import static test_utils.mockdata.FeasibilityRangeTestData.mockRange123;
import static test_utils.mockdata.FeasibilityRangeTestData.mockRange4;
import static test_utils.mockdata.FeasibilityRangeTestData.mockRange5;
import static test_utils.mockdata.FeasibilityRangeTestData.mockRange6;
import static test_utils.mockdata.FeasibilityRangeTestData.mockRange7;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg1;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg2;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg3;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg4;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg5;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg6;
import static test_utils.mockdata.ReferenceProgramTestData.mockRefProg7;

@SpringBootTest
@ActiveProfiles({"BciComputationTest"})
class BciComputerTest {
    private static final double EPSILON = 0.01;
    private static final List<BciTestCase> TEST_CASES = List.of(
            scenario(mockRefProg1(), mockRange123(), false, false,
                     Map.of("N1", values(false, 1000, 1000, 0, 0, 4000, 4000),
                            "N12", values(false, -1800, -1800, -2500, -2500, -500, -500),
                            "N8", values(false, 1000, 1000, -700, -700, 1300, 1300),
                            "N25", values(false, -200, -200, -1000, -1000, 0, 0))),
            scenario(mockRefProg2(), mockRange123(), true, false,
                     Map.of("N1", values(false, 500, 1103.45, 0, 0, 4000, 4000),
                            "N12", values(true, 500, -500, -2500, -2500, -500, -500),
                            "N8", values(false, -500, -189.66, -700, -700, 1300, 1300),
                            "N25", values(false, -500, -413.79, -1000, -1000, 0, 0))),
            scenario(mockRefProg3(), mockRange123(), true, false,
                     Map.of("N1", values(true, -1000, 0, 0, 0, 4000, 4000),
                            "N12", values(true, 0, -712.77, -2500, -2500, -500, -500),
                            "N8", values(false, 1000, 819.15, -700, -700, 1300, 1300),
                            "N25", values(false, 0, -106.38, -1000, -1000, 0, 0))),
            scenario(mockRefProg4(), mockRange4(), true, false,
                     Map.of("N1", values(true, -1500, 0, 0, 0, 4000, 4000),
                            "N12", values(true, 0, -1300, -2500, -2500, -500, -500),
                            "N8", values(false, 1000, 880, 700, 700, 2700, 2700),
                            "N25", values(false, 500, 420, 300, 300, 1300, 1300))),
            scenario(mockRefProg5(), mockRange5(), true, true,
                     Map.of("N1", values(true, -3000, -755.56, 0, -755.56, 4000, 4000),
                            "N12", values(false, -2200, -3577.78, -3200, -3577.78, -1200, -1200),
                            "N8", values(false, 1500, 922.22, 1300, 922.22, 3300, 3300),
                            "N25", values(false, 3700, 3411.11, 3600, 3411.11, 4600, 4600))),
            scenario(mockRefProg6(), mockRange6(), true, false,
                     Map.of("N1", values(true, 400, 200, -200, -200, 200, 200),
                            "N12", values(true, -400, -200, -200, -200, 200, 200),
                            "N8", values(false, 0, 0, -200, -200, 200, 200),
                            "N25", values(false, 0, 0, -1000, -1000, 1000, 1000))),
            scenario(mockRefProg7(), mockRange7(), true, true,
                     Map.of("N1", values(true, 800, 371.43, -200, -200, 200, 371.43),
                            "N12", values(false, 200, 371.43, -200, -200, 200, 371.43),
                            "N8", values(false, 200, 371.43, -200, -200, 200, 371.43),
                            "N25", values(false, -1200, -1114.29, -1400, -1400, -1200, -1114.29)))
    );
    @Autowired
    private RegionConfiguration regionConfiguration;

    private static BciTestCase scenario(final ReferenceProgram referenceProgram,
                                        final Map<String, Interval> feasibilityRange,
                                        final boolean bciActive,
                                        final boolean extendedRanges,
                                        final Map<String, ExpectedBciResults> expectedByCountry) {
        return new BciTestCase(referenceProgram, feasibilityRange, bciActive, extendedRanges, expectedByCountry);
    }

    @ParameterizedTest
    @FieldSource("TEST_CASES")
    void shouldApplyBci(final BciTestCase testCase) {
        assertApplyBci(testCase.referenceProgram,
                       testCase.feasibilityRange,
                       testCase.bciActive,
                       testCase.extendedRanges,
                       testCase.expectedByCountry);

    }

    void assertApplyBci(final ReferenceProgram referenceProgram,
                        final Map<String, Interval> feasibilityRange,
                        final boolean bciActive,
                        final boolean extendedRanges,
                        final Map<String, ExpectedBciResults> expectedByCountry) {
        final BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        final BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertEquals(result.bciActive(), bciActive);
        assertEquals(result.bciFeasibilityRangesExtended(), extendedRanges);
        final Map<String, BciAreaResults> results = result.bciResults();

        expectedByCountry.forEach((country, expected) -> {
            assertThat(results.get(country))
                    .hadBciApplied(expected.bciApplied)
                    .hasForecast(expected.forecast)
                    .hasTarget(expected.target)
                    .hasInFinalMax(expected.finalMax)
                    .hasInInitialMax(expected.initialMax)
                    .hasInFinalMin(expected.finalMin)
                    .hasInInitialMin(expected.initialMin);
        });

        assertRegionIsBalanced(results);
    }

    private void assertRegionIsBalanced(final Map<String, BciAreaResults> results) {
        double totalInRegionNetPosition = results
                .values()
                .stream()
                .map(BciAreaResults::getInRegionNetPositions)
                .mapToDouble(InRegionNetPositions::target)
                .sum();
        assertEquals(0, totalInRegionNetPosition, EPSILON);
    }

    @Configuration
    @Profile("BciComputationTest")
    static class ContextConfiguration {

        @Bean
        public RegionConfiguration regionConfiguration() {
            RegionConfiguration regionConfiguration = new RegionConfiguration();
            Map<String, String> areasId = new HashMap<>();
            areasId.put("N1", "ONE");
            areasId.put("N12", "TWELVE");
            areasId.put("N8", "EIGHT");
            areasId.put("N25", "TWENTYFIVE");
            regionConfiguration.setId("CE");
            regionConfiguration.setName("CE");
            regionConfiguration.setAreasIn(areasId);
            return regionConfiguration;
        }
    }

    private record BciTestCase(ReferenceProgram referenceProgram,
                               Map<String, Interval> feasibilityRange,
                               boolean bciActive,
                               boolean extendedRanges,
                               Map<String, ExpectedBciResults> expectedByCountry) {
    }

    record ExpectedBciResults(boolean bciApplied, double forecast, double target,
                              double initialMin, double finalMin,
                              double initialMax, double finalMax) {

        public static ExpectedBciResults values(final boolean bciApplied,
                                                final double forecast,
                                                final double target,
                                                final double initialMin,
                                                final double finalMin,
                                                final double initialMax,
                                                final double finalMax) {
            return new ExpectedBciResults(bciApplied, forecast, target, initialMin, finalMin, initialMax, finalMax);
        }
    }
}

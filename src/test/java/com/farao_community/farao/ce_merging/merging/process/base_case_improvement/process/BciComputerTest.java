/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.base_case_improvement.process;

import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.FlowByAreaMap;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.Interval;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciAreaResults;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.result.BciComputationResult;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.netpositions.InRegionNetPositions;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static test_utils.mockdata.FeasibilityRangeTestData.createFeasibilityRangeCase123;
import static test_utils.mockdata.FeasibilityRangeTestData.createFeasibilityRangeCase4;
import static test_utils.mockdata.FeasibilityRangeTestData.createFeasibilityRangeCase5;
import static test_utils.mockdata.FeasibilityRangeTestData.createFeasibilityRangeCase6;
import static test_utils.mockdata.FeasibilityRangeTestData.createFeasibilityRangeCase7;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase1;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase2;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase3;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase4;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase5;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase6;
import static test_utils.mockdata.ReferenceProgramTestData.createReferenceProgramCase7;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.assertions.BciAreaResultsAssert.assertThat;

@SpringBootTest
@ActiveProfiles({"BciComputationTest"})
class BciComputerTest {
    private static final double EPSILON = 0.01;

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

    @Autowired
    private RegionConfiguration regionConfiguration;

    @Test
    void computeBciCase1() {
        ReferenceProgram referenceProgram = createReferenceProgramCase1();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase123();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertFalse(result.bciActive());
        assertFalse(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadNoBciApplied()
            .hasInNpf(1000.0)
            .hasGlobalNpf(1000.0)
            .hasInTargetNp(1000.0)
            .hasGlobalTargetNp(1000.0);

        assertThat(results.get("N12"))
            .hadNoBciApplied()
            .hasInNpf(-1800.0)
            .hasGlobalNpf(-1800.0)
            .hasInTargetNp(-1800.0)
            .hasGlobalTargetNp(-1800.0);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(1000.0)
            .hasGlobalNpf(1000.0)
            .hasInTargetNp(1000.0)
            .hasGlobalTargetNp(1000.0);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(-200.0)
            .hasGlobalNpf(-200.0)
            .hasInTargetNp(-200.0)
            .hasGlobalTargetNp(-200.0);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase2() {
        ReferenceProgram referenceProgram = createReferenceProgramCase2();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase123();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertFalse(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadNoBciApplied()
            .hasInNpf(500)
            .hasGlobalNpf(500)
            .hasInTargetNp(1103.45)
            .hasGlobalTargetNp(1103.45);

        assertThat(results.get("N12"))
            .hadBciApplied()
            .hasInNpf(500)
            .hasGlobalNpf(500)
            .hasInTargetNp(-500.0)
            .hasGlobalTargetNp(-500.0);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(-500)
            .hasGlobalNpf(-500)
            .hasInTargetNp(-189.66)
            .hasGlobalTargetNp(-189.66);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(-500.0)
            .hasGlobalNpf(-500.0)
            .hasInTargetNp(-413.79)
            .hasGlobalTargetNp(-413.79);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase3() {
        ReferenceProgram referenceProgram = createReferenceProgramCase3();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase123();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertFalse(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadBciApplied()
            .hasInNpf(-1000)
            .hasGlobalNpf(-1000)
            .hasInTargetNp(0)
            .hasGlobalTargetNp(0);

        assertThat(results.get("N12"))
            .hadBciApplied()
            .hasInNpf(0)
            .hasGlobalNpf(0)
            .hasInTargetNp(-712.77)
            .hasGlobalTargetNp(-712.77);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(1000)
            .hasGlobalNpf(1000)
            .hasInTargetNp(819.15)
            .hasGlobalTargetNp(819.15);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(0)
            .hasGlobalNpf(0)
            .hasInTargetNp(-106.38)
            .hasGlobalTargetNp(-106.38);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase4() {
        ReferenceProgram referenceProgram = createReferenceProgramCase4();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase4();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertFalse(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadBciApplied()
            .hasInNpf(-1500)
            .hasGlobalNpf(-1500)
            .hasInTargetNp(0)
            .hasGlobalTargetNp(0);

        assertThat(results.get("N12"))
            .hadBciApplied()
            .hasInNpf(0)
            .hasGlobalNpf(0)
            .hasInTargetNp(-1300)
            .hasGlobalTargetNp(-1300);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(1000)
            .hasGlobalNpf(1000)
            .hasInTargetNp(880)
            .hasGlobalTargetNp(880);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(500)
            .hasGlobalNpf(500)
            .hasInTargetNp(420)
            .hasGlobalTargetNp(420);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase5() {
        ReferenceProgram referenceProgram = createReferenceProgramCase5();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase5();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertTrue(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadBciApplied()
            .hasInNpf(-3000.0)
            .hasGlobalNpf(-3000.0)
            .hasInTargetNp(-755.56)
            .hasGlobalTargetNp(-755.56);

        assertThat(results.get("N12"))
            .hadNoBciApplied()
            .hasInNpf(-2200.0)
            .hasGlobalNpf(-2200.0)
            .hasInTargetNp(-3577.78)
            .hasGlobalTargetNp(-3577.78);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(1500.0)
            .hasGlobalNpf(1500.0)
            .hasInTargetNp(922.22)
            .hasGlobalTargetNp(922.22);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(3700.0)
            .hasGlobalNpf(3700.0)
            .hasInTargetNp(3411.11)
            .hasGlobalTargetNp(3411.11);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase6() {
        ReferenceProgram referenceProgram = createReferenceProgramCase6();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase6();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertFalse(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadBciApplied()
            .hasInNpf(400.0)
            .hasGlobalNpf(400.0)
            .hasInTargetNp(200.0)
            .hasGlobalTargetNp(200.0);

        assertThat(results.get("N12"))
            .hadBciApplied()
            .hasInNpf(-400.0)
            .hasGlobalNpf(-400.0)
            .hasInTargetNp(-200.0)
            .hasGlobalTargetNp(-200.0);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(0.0)
            .hasGlobalNpf(0.0)
            .hasInTargetNp(0.0)
            .hasGlobalTargetNp(0.0);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(0.0)
            .hasGlobalNpf(0.0)
            .hasInTargetNp(0.0)
            .hasGlobalTargetNp(0.0);

        assertRegionIsBalanced(results);
    }

    @Test
    void computeBciCase7() {
        ReferenceProgram referenceProgram = createReferenceProgramCase7();
        Map<String, Interval> feasibilityRange = createFeasibilityRangeCase7();
        BciComputer bciComputer = new BciComputer(regionConfiguration, referenceProgram, feasibilityRange);

        BciComputationResult result = bciComputer.run(new FlowByAreaMap(), 0, 0);
        assertTrue(result.bciActive());
        assertTrue(result.bciFeasibilityRangesExtended());
        Map<String, BciAreaResults> results = result.bciResults();

        assertThat(results.get("N1"))
            .hadBciApplied()
            .hasInNpf(800.0)
            .hasGlobalNpf(800.0)
            .hasInTargetNp(371.43)
            .hasGlobalTargetNp(371.43)
            .hasInInitialMin(-200.0)
            .hasInInitialMax(200.0)
            .hasInFinalMin(-200.0)
            .hasInFinalMax(371.43);

        assertThat(results.get("N12"))
            .hadNoBciApplied()
            .hasInNpf(200.0)
            .hasGlobalNpf(200.0)
            .hasInTargetNp(371.43)
            .hasGlobalTargetNp(371.43)
            .hasInInitialMin(-200.0)
            .hasInInitialMax(200.0)
            .hasInFinalMin(-200.0)
            .hasInFinalMax(371.43);

        assertThat(results.get("N8"))
            .hadNoBciApplied()
            .hasInNpf(200.0)
            .hasGlobalNpf(200.0)
            .hasInTargetNp(371.43)
            .hasGlobalTargetNp(371.43)
            .hasInInitialMin(-200.0)
            .hasInInitialMax(200.0)
            .hasInFinalMin(-200.0)
            .hasInFinalMax(371.43);

        assertThat(results.get("N25"))
            .hadNoBciApplied()
            .hasInNpf(-1200.0)
            .hasGlobalNpf(-1200.0)
            .hasInTargetNp(-1114.29)
            .hasGlobalTargetNp(-1114.29)
            .hasInInitialMin(-1400.0)
            .hasInInitialMax(-1200.0)
            .hasInFinalMin(-1400.0)
            .hasInFinalMax(-1114.29);

        assertRegionIsBalanced(results);
    }

    private static void assertRegionIsBalanced(final Map<String, BciAreaResults> results) {
        double totalInRegionNetPosition = results
            .values()
            .stream()
            .map(BciAreaResults::getInRegionNetPositions)
            .mapToDouble(InRegionNetPositions::target)
            .sum();
        assertEquals(0, totalInRegionNetPosition, EPSILON);
    }
}

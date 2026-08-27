/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.alegro;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
import com.farao_community.farao.ce_merging.merging.process.forecast_netpositions.ReferenceProgram;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class AlegroServiceTest {
    private static final String FORECAST_REFERENCE_PROGRAM_PATH = "src/test/resources/alegro/forecastReferenceProgram.json";

    @Autowired
    private AlegroService alegroService;

    @Test
    void shouldThrowExceptionWhenTwoFlowsHaveTheSameSignAndLoadsNotBelowThreshold() {
        assertThrows(
                CeMergingException.class,
                () -> alegroService.checkFlowDirection(200, 210, 0)
        );
    }

    @Test
    void shouldNotThrowExceptionWhenTwoFlowsHaveTheSameSignAndLoadsBelowThreshold() {
        alegroService.checkFlowDirection(5, 5, 5);
    }

    @Test
    void shouldThrowExceptionWhenTheDifferenceBetweenFlowsExceedTheThreshold() {
        assertThrows(
                CeMergingException.class,
                () -> alegroService.checkFlowCompliance(200, 210, 5)
        );
    }

    @Test
    void shouldThrowExceptionWhenTheGapNpfInitialFlowExceedTheThreshold() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        assertThrows(
                CeMergingException.class,
                () -> alegroService.getAlegroNetPositions(referenceProgram, false, 200, -200, 5)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "false, false, true",
        "true, false, true",
        "false, true, true",
        "true, true, false"
    })
    void shouldDetectAlegroOutageBasedOnXNodesConnection(final boolean albeConnected, final boolean aldeConnected, final boolean expectedOutage) {
        final DanglingLine albe = mock(DanglingLine.class);
        final DanglingLine alde = mock(DanglingLine.class);

        final Terminal albeTerminal = mock(Terminal.class);
        final Terminal aldeTerminal = mock(Terminal.class);

        when(albe.getTerminal()).thenReturn(albeTerminal);
        when(alde.getTerminal()).thenReturn(aldeTerminal);

        when(albeTerminal.isConnected()).thenReturn(albeConnected);
        when(aldeTerminal.isConnected()).thenReturn(aldeConnected);

        assertEquals(expectedOutage, alegroService.isAlegroInOutage(List.of(albe, alde)));
    }

    @Test
    void shouldGetCorrectAlegroNetPositions() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        final AlegroData alegroData = alegroService.getAlegroNetPositions(referenceProgram, false, -200, 200, 10);
        assertEquals(200, alegroData.aldeFlows().initialFlow(), 0.01);
        assertEquals(210, alegroData.aldeFlows().targetFlow(), 0.01);
        assertEquals(10, alegroData.aldeFlows().gapNpfInitialFlow(), 0.01);
        assertEquals(-200, alegroData.albeFlows().initialFlow(), 0.01);
        assertEquals(-210, alegroData.albeFlows().targetFlow(), 0.01);
        assertEquals(-10, alegroData.albeFlows().gapNpfInitialFlow(), 0.01);
    }

    @Test
    void shouldNotCheckFlowGapWhenAlegroIsInOutage() {
        final ReferenceProgram referenceProgram = getReferenceProgram();
        assertDoesNotThrow(
                () ->
                alegroService.getAlegroNetPositions(referenceProgram, true, 1000, -1000, 5)
        );
    }

    private ReferenceProgram getReferenceProgram() {
        return JsonUtils.read(ReferenceProgram.class, FORECAST_REFERENCE_PROGRAM_PATH);
    }

}

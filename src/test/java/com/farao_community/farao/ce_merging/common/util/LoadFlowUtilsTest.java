/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.function.Supplier;

import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class LoadFlowUtilsTest {

    @Test
    void shouldConvergeOnDcFallbackAfterAcDivergence() {
        final Network network = mock(Network.class);
        when(network.getId()).thenReturn("test-net");

        final LoadFlow.Runner runner = mock(LoadFlow.Runner.class);
        final Supplier<LoadFlow.Runner> runnerSupplier = () -> runner;

        final LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.setDc(false);

        final LoadFlowResult divergeResult = mock(LoadFlowResult.class);
        when(divergeResult.getComponentResults()).thenReturn(Collections.emptyList());

        final LoadFlowResult convergeResult = mock(LoadFlowResult.class);
        final LoadFlowResult.ComponentResult mainComponentConverged = mock(LoadFlowResult.ComponentResult.class);
        when(mainComponentConverged.getSynchronousComponentNum()).thenReturn(MAIN_NUM);
        when(mainComponentConverged.getStatus()).thenReturn(CONVERGED);
        when(convergeResult.getComponentResults()).thenReturn(Collections.singletonList(mainComponentConverged));

        when(runner.run(eq(network), any(LoadFlowParameters.class)))
            .thenReturn(divergeResult)
            .thenReturn(convergeResult);

        assertDoesNotThrow(() -> LoadFlowUtils.runLoadflow(network, runnerSupplier, parameters));

        assertFalse(parameters.isDc());
        verify(runner, times(2)).run(eq(network), any(LoadFlowParameters.class));
    }

    @Test
    void shouldThrowExceptionWhenBothAcAndDcDiverge() {
        final Network network = mock(Network.class);
        when(network.getId()).thenReturn("test-net");

        final LoadFlow.Runner runner = mock(LoadFlow.Runner.class);
        Supplier<LoadFlow.Runner> runnerSupplier = () -> runner;

        final LoadFlowParameters parameters = new LoadFlowParameters();
        parameters.setDc(false);

        final LoadFlowResult divergeResult = mock(LoadFlowResult.class);
        when(divergeResult.getComponentResults()).thenReturn(Collections.emptyList());
        when(runner.run(eq(network), any(LoadFlowParameters.class))).thenReturn(divergeResult);

        assertThatThrownBy(() -> LoadFlowUtils.runLoadflow(network, runnerSupplier, parameters))
            .isValidServiceException()
            .hasMessageContaining("DC load flow diverged on network test-net");

        verify(runner, times(2)).run(eq(network), any(LoadFlowParameters.class));
    }
}

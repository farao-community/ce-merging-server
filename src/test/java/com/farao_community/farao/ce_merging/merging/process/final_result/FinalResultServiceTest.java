/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.final_result;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesCalculation;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.execution_logs.Logs;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.LoadFlowRunParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Collections;

import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.LOAD_FLOW_ON_FINAL_CGM_LOGS;
import static com.farao_community.farao.ce_merging.merging.task.enums.TaskStatus.CREATED;
import static com.powsybl.iidm.network.ComponentConstants.MAIN_NUM;
import static com.powsybl.loadflow.LoadFlowResult.ComponentResult.Status.CONVERGED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.taskWithIdAndStatus;

class FinalResultServiceTest {

    private CeMergingConfiguration configuration;
    private LoadFlow.Runner loadFlowRunner;
    private FinalResultService service;
    private MergingTask task;

    @BeforeEach
    void setUp() {
        configuration = mock(CeMergingConfiguration.class);
        loadFlowRunner = mock(LoadFlow.Runner.class);
        final XnodesCalculation xnodesCalculation = mock(XnodesCalculation.class);
        service = new FinalResultService(configuration, () -> loadFlowRunner, xnodesCalculation);

        task = taskWithIdAndStatus(1L, CREATED);
        task.getOutputs().setCgm(new SavedFile("cgm.xiidm", "/path/to/cgm.xiidm", "cgm"));
        task.getConfigurations().setLoadFlowParameters(new LoadFlowParameters());
    }

    @Test
    void shouldRunLoadFlowAndSaveFinalCgmResults() {
        final Network network = mock(Network.class);
        final LoadFlowResult loadFlowResult = mock(LoadFlowResult.class);
        final LoadFlowResult.ComponentResult componentResult = mock(LoadFlowResult.ComponentResult.class);
        when(network.getCountries()).thenReturn(Collections.emptySet());
        when(componentResult.getSynchronousComponentNum()).thenReturn(MAIN_NUM);
        when(componentResult.getStatus()).thenReturn(CONVERGED);
        when(componentResult.getSlackBusResults()).thenReturn(Collections.emptyList());
        when(loadFlowResult.getComponentResults()).thenReturn(Collections.singletonList(componentResult));
        when(loadFlowResult.isOk()).thenReturn(true);
        when(loadFlowRunner.run(eq(network), any(LoadFlowRunParameters.class))).thenReturn(loadFlowResult);

        try (MockedStatic<Network> networkMock = mockStatic(Network.class);
             MockedStatic<FileStorageUtils> fileStorageUtilsMock = mockStatic(FileStorageUtils.class)) {
            networkMock.when(() -> Network.read(anyString())).thenReturn(network);

            service.computeCgmResults(task);

            verify(loadFlowRunner).run(eq(network), any(LoadFlowRunParameters.class));

            final ArgumentCaptor<Logs> logsCaptor = ArgumentCaptor.forClass(Logs.class);
            final ArgumentCaptor<FinalCgmResult> cgmResultCaptor = ArgumentCaptor.forClass(FinalCgmResult.class);
            fileStorageUtilsMock.verify(() -> saveArtifactFile(eq(LOAD_FLOW_ON_FINAL_CGM_LOGS), logsCaptor.capture(),
                                                               eq(task), eq(configuration)));
            fileStorageUtilsMock.verify(() -> saveArtifactFile(eq(CGM_NET_POSITIONS_FILE), cgmResultCaptor.capture(),
                                                               eq(task), eq(configuration)));

            assertThat(logsCaptor.getValue()).isNotNull();
            assertThat(cgmResultCaptor.getValue().getLoadFlowResults().getCgmFileName()).isEqualTo("cgm.xiidm");
            assertThat(cgmResultCaptor.getValue().getLoadFlowResults().isLoadflowStatus()).isTrue();
        }
    }
}

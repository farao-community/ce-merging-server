/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.global_grid_configurations.GlobalGridConfigurationService;
import com.farao_community.farao.ce_merging.merging.post_process.merging_logs.MergingLogsCalculationService;
import com.farao_community.farao.ce_merging.merging.process.alegro.AlegroService;
import com.farao_community.farao.ce_merging.merging.process.balances_adjustment.BalancesAdjustmentService;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.BaseCaseImprovementService;
import com.farao_community.farao.ce_merging.merging.process.dk_renaming.DKRenamingService;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.FinalCgmService;
import com.farao_community.farao.ce_merging.merging.process.forecast_netpositions.ForecastNetPositionService;
import com.farao_community.farao.ce_merging.merging.process.german_pre_merge.GermanPreMergeService;
import com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskQualityCheckService;
import com.farao_community.farao.ce_merging.merging.process.hvdc_alignment.HvdcXNodeAlignmentService;
import com.farao_community.farao.ce_merging.merging.process.monita.MonitaService;
import com.farao_community.farao.ce_merging.merging.process.netpositions.NetPositionService;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstSpecialService;
import com.farao_community.farao.ce_merging.merging.process.recessivity.RecessivityService;
import com.farao_community.farao.ce_merging.merging.process.slack_compensation.SlackCompensationService;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.TargetNetPositionsCalculationService;
import com.farao_community.farao.ce_merging.merging.process.topological_merge.TopologicalMergeService;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesService;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergingServiceTest {

    @Mock
    private AlegroService alegroService;

    @Mock
    private GlobalGridConfigurationService gridConfigurationService;

    @Mock
    private BaseCaseImprovementService baseCaseImprovementService;

    @Mock
    private BalancesAdjustmentService balancesAdjustmentService;

    @Mock
    private XnodesService xnodesService;

    @Mock
    private ForecastNetPositionService forecastNetPositionService;

    @Mock
    private GermanPreMergeService germanPreMergeService;

    @Mock
    private DKRenamingService dkRenamingService;

    @Mock
    private HvdcXNodeAlignmentService hvdcXNodeAlignmentService;

    @Mock
    private MonitaService monitaService;

    @Mock
    private NetPositionService netPositionService;

    @Mock
    private TopologicalMergeService topologicalMergeService;

    @Mock
    private RecessivityService recessivityService;

    @Mock
    private GlskQualityCheckService glskQualityCheckService;

    @Mock
    private TargetNetPositionsCalculationService targetNetPositionsCalculationService;

    @Mock
    private PstSpecialService pstSpecialService;

    @Mock
    private SlackCompensationService slackCompensationService;

    @Mock
    private FinalCgmService finalCgmService;

    @Mock
    private MergingLogsCalculationService mergingLogsCalculationService;

    @Mock
    private MergingTask task;

    @Mock
    private Inputs inputs;

    @InjectMocks
    private MergingService mergingService;

    @BeforeEach
    void setUp() {
        when(task.getInputs()).thenReturn(inputs);
    }

    @Test
    void shouldRunMergingWithoutInternalHvdc() throws IOException {
        when(inputs.getMergingWithInternalHvdc()).thenReturn(false);

        mergingService.run(task);

        verify(gridConfigurationService).setConfigurations(task);

        verify(xnodesService).checkIgmsStatus(task);
        verify(forecastNetPositionService).importForecastNetPosition(task);
        verify(germanPreMergeService).preMergeGermanCountries(task);
        verify(dkRenamingService).renameDkCountry(task);
        verify(hvdcXNodeAlignmentService).applyHvdcXNodeAlignment(task);
        verify(hvdcXNodeAlignmentService).setZeroFlowNodes(task);
        verify(monitaService).renameNode(task);

        verify(netPositionService).computeInitialNetPositions(task);
        verify(topologicalMergeService).mergeInitialIgms(task);
        verify(recessivityService).applyRecessivity(task);

        verify(glskQualityCheckService).runQualityCheck(task);
        verify(baseCaseImprovementService).computeTargetNetPositions(task);

        verify(targetNetPositionsCalculationService).computeTargetNetPositions(task);
        verify(balancesAdjustmentService).shiftCgm(task);
        verify(pstSpecialService).fixPst(task);
        verify(slackCompensationService).compensateFinalCgmSlackImbalance(task);
        verify(finalCgmService).computeFinalCgmResult(task);
        verify(mergingLogsCalculationService).computeMergingLogs(task);

        verifyNoInteractions(alegroService);
    }

    @Test
    void shouldRunMergingWithInternalHvdc() throws IOException {
        when(inputs.getMergingWithInternalHvdc()).thenReturn(true);

        mergingService.run(task);

        verify(gridConfigurationService).setConfigurations(task);

        verify(xnodesService).checkIgmsStatus(task);
        verify(forecastNetPositionService).importForecastNetPosition(task);
        verify(germanPreMergeService).preMergeGermanCountries(task);
        verify(dkRenamingService).renameDkCountry(task);
        verify(hvdcXNodeAlignmentService).applyHvdcXNodeAlignment(task);
        verify(hvdcXNodeAlignmentService).setZeroFlowNodes(task);
        verify(monitaService).renameNode(task);

        verify(netPositionService).computeInitialNetPositions(task);
        verify(topologicalMergeService).mergeInitialIgms(task);
        verify(recessivityService).applyRecessivity(task);

        verify(alegroService).checkAlegroXnodesQuality(task);
        verify(glskQualityCheckService).runQualityCheck(task);
        verify(baseCaseImprovementService).computeTargetNetPositions(task);
        verify(alegroService).updateAlegroP0(task);

        verify(targetNetPositionsCalculationService).computeTargetNetPositions(task);
        verify(balancesAdjustmentService).shiftCgm(task);
        verify(pstSpecialService).fixPst(task);
        verify(slackCompensationService).compensateFinalCgmSlackImbalance(task);
        verify(finalCgmService).computeFinalCgmResult(task);
        verify(mergingLogsCalculationService).computeMergingLogs(task);
    }

    @Test
    void shouldExecuteMergingStepsInCorrectOrder() throws IOException {
        when(inputs.getMergingWithInternalHvdc()).thenReturn(true);

        mergingService.run(task);

        final InOrder inOrder = inOrder(
                gridConfigurationService,
                xnodesService,
                forecastNetPositionService,
                germanPreMergeService,
                dkRenamingService,
                hvdcXNodeAlignmentService,
                monitaService,
                netPositionService,
                topologicalMergeService,
                recessivityService,
                alegroService,
                glskQualityCheckService,
                baseCaseImprovementService,
                targetNetPositionsCalculationService,
                balancesAdjustmentService,
                pstSpecialService,
                slackCompensationService,
                finalCgmService,
                mergingLogsCalculationService
        );

        inOrder.verify(gridConfigurationService).setConfigurations(task);

        inOrder.verify(xnodesService).checkIgmsStatus(task);
        inOrder.verify(forecastNetPositionService).importForecastNetPosition(task);
        inOrder.verify(germanPreMergeService).preMergeGermanCountries(task);
        inOrder.verify(dkRenamingService).renameDkCountry(task);
        inOrder.verify(hvdcXNodeAlignmentService).applyHvdcXNodeAlignment(task);
        inOrder.verify(hvdcXNodeAlignmentService).setZeroFlowNodes(task);
        inOrder.verify(monitaService).renameNode(task);

        inOrder.verify(netPositionService).computeInitialNetPositions(task);
        inOrder.verify(topologicalMergeService).mergeInitialIgms(task);
        inOrder.verify(recessivityService).applyRecessivity(task);

        inOrder.verify(alegroService).checkAlegroXnodesQuality(task);
        inOrder.verify(glskQualityCheckService).runQualityCheck(task);
        inOrder.verify(baseCaseImprovementService).computeTargetNetPositions(task);
        inOrder.verify(alegroService).updateAlegroP0(task);

        inOrder.verify(targetNetPositionsCalculationService).computeTargetNetPositions(task);
        inOrder.verify(balancesAdjustmentService).shiftCgm(task);
        inOrder.verify(pstSpecialService).fixPst(task);
        inOrder.verify(slackCompensationService).compensateFinalCgmSlackImbalance(task);
        inOrder.verify(finalCgmService).computeFinalCgmResult(task);
        inOrder.verify(mergingLogsCalculationService).computeMergingLogs(task);
    }
}

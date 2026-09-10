/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.global_grid_configurations.GlobalGridConfigurationService;
import com.farao_community.farao.ce_merging.merging.post_process.export_results.ExportTaskResultsService;
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
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class MergingService {
    private final AlegroService alegroService;
    private final BalancesAdjustmentService balancesAdjustmentService;
    private final BaseCaseImprovementService baseCaseImprovementService;
    private final DKRenamingService dkRenamingService;
    private final ExportTaskResultsService exportResultsService;
    private final FinalCgmService finalCgmService;
    private final ForecastNetPositionService forecastNetPositionService;
    private final GermanPreMergeService germanPreMergeService;
    private final GlobalGridConfigurationService gridConfigurationService;
    private final GlskQualityCheckService glskQualityCheckService;
    private final HvdcXNodeAlignmentService hvdcXNodeAlignmentService;
    private final MergingLogsCalculationService mergingLogsCalculationService;
    private final MonitaService monitaService;
    private final NetPositionService netPositionService;
    private final PstSpecialService pstSpecialService;
    private final RecessivityService recessivityService;
    private final SlackCompensationService slackCompensationService;
    private final TargetNetPositionsCalculationService targetNetPositionsCalculationService;
    private final TopologicalMergeService topologicalMergeService;
    private final XnodesService xnodesService;

    public MergingService(
            AlegroService alegroService,
            BalancesAdjustmentService balancesAdjustmentService,
            BaseCaseImprovementService baseCaseImprovementService,
            DKRenamingService dkRenamingService,
            ExportTaskResultsService exportResultsService,
            FinalCgmService finalCgmService,
            ForecastNetPositionService forecastNetPositionService,
            GermanPreMergeService germanPreMergeService,
            GlobalGridConfigurationService gridConfigurationService,
            GlskQualityCheckService glskQualityCheckService,
            HvdcXNodeAlignmentService hvdcXNodeAlignmentService,
            MergingLogsCalculationService mergingLogsCalculationService,
            MonitaService monitaService,
            NetPositionService netPositionService,
            PstSpecialService pstSpecialService,
            RecessivityService recessivityService,
            SlackCompensationService slackCompensationService,
            TargetNetPositionsCalculationService targetNetPositionsCalculationService,
            TopologicalMergeService topologicalMergeService,
            XnodesService xnodesService) {

        this.alegroService = alegroService;
        this.balancesAdjustmentService = balancesAdjustmentService;
        this.baseCaseImprovementService = baseCaseImprovementService;
        this.dkRenamingService = dkRenamingService;
        this.exportResultsService = exportResultsService;
        this.finalCgmService = finalCgmService;
        this.forecastNetPositionService = forecastNetPositionService;
        this.germanPreMergeService = germanPreMergeService;
        this.gridConfigurationService = gridConfigurationService;
        this.glskQualityCheckService = glskQualityCheckService;
        this.hvdcXNodeAlignmentService = hvdcXNodeAlignmentService;
        this.mergingLogsCalculationService = mergingLogsCalculationService;
        this.monitaService = monitaService;
        this.netPositionService = netPositionService;
        this.pstSpecialService = pstSpecialService;
        this.recessivityService = recessivityService;
        this.slackCompensationService = slackCompensationService;
        this.targetNetPositionsCalculationService = targetNetPositionsCalculationService;
        this.topologicalMergeService = topologicalMergeService;
        this.xnodesService = xnodesService;
    }

    public void run(final MergingTask task) throws IOException {
        configure(task);
        prepareInputs(task);
        process(task);
        generateResults(task);
    }

    private void configure(final MergingTask task) {
        gridConfigurationService.setConfigurations(task);
    }

    private void process(final MergingTask task) throws IOException {
        netPositionService.computeInitialNetPositions(task);
        topologicalMergeService.mergeInitialIgms(task);
        recessivityService.applyRecessivity(task);
        final boolean isMergingWithInternalHvdc = task.getInputs().getMergingWithInternalHvdc();
        if (isMergingWithInternalHvdc) {
            alegroService.checkAlegroXnodesQuality(task);
        }
        glskQualityCheckService.runQualityCheck(task);
        baseCaseImprovementService.computeTargetNetPositions(task);
        if (isMergingWithInternalHvdc) {
            alegroService.updateAlegroP0(task);
        }
        targetNetPositionsCalculationService.computeTargetNetPositions(task);
        balancesAdjustmentService.shiftCgm(task);
        pstSpecialService.fixPst(task);
        slackCompensationService.compensateFinalCgmSlackImbalance(task);
        finalCgmService.computeFinalCgmResult(task);
    }

    private void prepareInputs(final MergingTask task) {
        xnodesService.checkIgmsStatus(task);
        forecastNetPositionService.importForecastNetPosition(task);
        germanPreMergeService.preMergeGermanCountries(task);
        dkRenamingService.renameDkCountry(task);
        hvdcXNodeAlignmentService.applyHvdcXNodeAlignment(task);
        hvdcXNodeAlignmentService.setZeroFlowNodes(task);
        monitaService.renameNode(task);
    }

    private void generateResults(final MergingTask task) {
        mergingLogsCalculationService.computeMergingLogs(task);
        exportResultsService.generateOutputFiles(task);
    }
}

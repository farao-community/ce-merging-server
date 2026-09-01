/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging;

import com.farao_community.farao.ce_merging.global_grid_configurations.GlobalGridConfigurationService;
import com.farao_community.farao.ce_merging.merging.process.alegro.AlegroService;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.BaseCaseImprovementService;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.AlegroData;
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
import com.farao_community.farao.ce_merging.merging.process.topologicalMerge.TopologicalMergeService;
import com.farao_community.farao.ce_merging.merging.process.xnode.XnodesService;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import org.springframework.stereotype.Service;

@Service
public class MergingService {
    private final AlegroService alegroService;
    private final GlobalGridConfigurationService gridConfigurationService;
    private final BaseCaseImprovementService baseCaseImprovementService;
    private final XnodesService xnodesService;
    private final ForecastNetPositionService forecastNetPositionService;
    private final GermanPreMergeService germanPreMergeService;
    private final DKRenamingService dkRenamingService;
    private final HvdcXNodeAlignmentService hvdcXNodeAlignmentService;
    private final MonitaService monitaService;
    private final NetPositionService netPositionService;
    private final TopologicalMergeService topologicalMergeService;
    private final RecessivityService recessivityService;
    private final GlskQualityCheckService glskQualityCheckService;
    private final TargetNetPositionsCalculationService targetNetPositionsCalculationService;
    private final PstSpecialService pstSpecialService;
    private final SlackCompensationService slackCompensationService;
    private final FinalCgmService finalCgmService;


    public MergingService(AlegroService alegroService, BaseCaseImprovementService baseCaseImprovementService, GlobalGridConfigurationService gridConfigurationService, XnodesService xnodesService, ForecastNetPositionService forecastNetPositionService, GermanPreMergeService germanPreMergeService, DKRenamingService dkRenamingService, HvdcXNodeAlignmentService hvdcXNodeAlignmentService, MonitaService monitaService, NetPositionService netPositionService, TopologicalMergeService topologicalMergeService, RecessivityService recessivityService, GlskQualityCheckService glskQualityCheckService, TargetNetPositionsCalculationService targetNetPositionsCalculationService, PstSpecialService pstSpecialService, SlackCompensationService slackCompensationService, FinalCgmService finalCgmService) {
        this.alegroService = alegroService;
        this.baseCaseImprovementService = baseCaseImprovementService;
        this.gridConfigurationService = gridConfigurationService;
        this.xnodesService = xnodesService;
        this.forecastNetPositionService = forecastNetPositionService;
        this.germanPreMergeService = germanPreMergeService;
        this.dkRenamingService = dkRenamingService;
        this.hvdcXNodeAlignmentService = hvdcXNodeAlignmentService;
        this.monitaService = monitaService;
        this.netPositionService = netPositionService;
        this.topologicalMergeService = topologicalMergeService;
        this.recessivityService = recessivityService;
        this.glskQualityCheckService = glskQualityCheckService;
        this.targetNetPositionsCalculationService = targetNetPositionsCalculationService;
        this.pstSpecialService = pstSpecialService;
        this.slackCompensationService = slackCompensationService;
        this.finalCgmService = finalCgmService;
    }

    public void run(final MergingTask task) {
        gridConfigurationService.setConfigurations(task);
        xnodesService.checkIgmsStatus(task);
        forecastNetPositionService.importForecastNetPosition(task);
        germanPreMergeService.preMergeGermanCountries(task);
        dkRenamingService.renameDkCountry(task);
        hvdcXNodeAlignmentService.applyHvdcXNodeAlignment(task);
        hvdcXNodeAlignmentService.setZeroFlowNodes(task);
        monitaService.renameNode(task);
        netPositionService.computeInitialNetPositions(task);
        topologicalMergeService.mergeInitialIgms(task);
        recessivityService.applyRecessivity(task);
        if (task.getInputs().getMergingWithInternalHvdc()) {
           alegroService.checkAlegroXnodesQuality(task);
            glskQualityCheckService.runQualityCheck(task);
            baseCaseImprovementService.computeTargetNetPositions(task);
           alegroService.updateAlegroP0(task);
        } else {
            glskQualityCheckService.runQualityCheck(task);
            baseCaseImprovementService.computeTargetNetPositions(task);
        }
        targetNetPositionsCalculationService.computeTargetNetPositions(task);
       // balancesAdjustmentService.shiftCgm(task);
        pstSpecialService.fixPst(task);
        slackCompensationService.compensateFinalCgmSlackImbalance(task);
        finalCgmService.computeFinalCgmResult(task);
    }

    private void prepareInputs(MergingTask task) {
        forecastNetPositionService.importForecastNetPosition(task);
        germanPreMergeService.preMergeGermanCountries(task);
        dkRenamingService.renameDkCountry(task);
    }

}

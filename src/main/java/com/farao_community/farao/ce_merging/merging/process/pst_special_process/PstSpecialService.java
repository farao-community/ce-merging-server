/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.pst_special_process;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.PstOutput;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFile;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactNetwork;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlow;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInOutage;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getTargetFlow;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.hasTargetFlow;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.regulatePst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.PADRICIANO;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.forAllSpecialPst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.toPstMap;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static com.powsybl.iidm.network.Country.SI;
import static com.powsybl.iidm.network.util.Networks.applySolvedTapPositionAndSolvedSectionCount;

@Service
public class PstSpecialService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PstSpecialService.class);
    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    private static final String DIVACA_PADRICIANO_DANGLING_LINE = "LDIVAC2[0-9A-Z] XPA_DI21 1";
    private static final String DIVACA_REDIPULGIA_DANGLING_LINE = "LDIVAC1[0-9A-Z] XRE_DI11 1";
    private static final String DIVACA_PADRICIANO_LINE = "LDIVAC2[0-9A-Z] XPA_DI21 1 \\+ XPA_DI21 IPDRV12[0-9A-Z] 1";
    private static final String DIVACA_REDIPULGIA_LINE = "LDIVAC1[0-9A-Z] XRE_DI11 1 \\+ XRE_DI11 IRDPVA11 1";
    private static final double DIVACA_PADRICIANO_TARGET_FLOW = 150;

    public PstSpecialService(final CeMergingConfiguration configuration,
                             final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.configuration = configuration;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }

    public void fixPst(final MergingTask task) {
        try {
            doFixPst(task);
        } catch (final Exception e) {
            final String errorMessage = String.format("Error during fix PST special process for task '%d', cause : %s",
                                                      task.getId(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void doFixPst(final MergingTask task) {
        // prepare all the data
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        final SavedFile cgmFile = task.getArtifacts().getFile(BALANCED_CGM_FILE);
        final Network cgm = Network.read(cgmFile.getPath());
        final PstOutput pstOutput = new PstOutput();

        final Map<SpecialPst, TwoWindingsTransformer> pstsInIgms = toPstMap(
                pst -> pst.findInNetwork(task.getIgm(pst.getCountry()))
        );
        final Map<SpecialPst, String> pstIds = toPstMap(pst -> pstsInIgms.get(pst).getId());

        fillPstOutputsFromIgms(task, pstIds, pstOutput, loadFlowParameters);

        // handle IT-SI border Special PSTs
        applyDivacaPadricianoProcess(cgm.getTwoWindingsTransformer(pstIds.get(DIVACA)),
                                     cgm.getTwoWindingsTransformer(pstIds.get(PADRICIANO)),
                                     pstOutput);

        // save results
        fillPstOutputsFromCgm(cgm, pstIds, pstOutput, loadFlowParameters);
        saveArtifactFile(PST_OUTPUT_FILE, pstOutput, task, configuration);
        saveArtifactNetwork(CGM_FILE_AFTER_PST, cgm, task, UCTE_FORMAT, configuration);

    }

    private void applyDivacaPadricianoProcess(final TwoWindingsTransformer divaca,
                                              final TwoWindingsTransformer padriciano,
                                              final PstOutput pstOutput) {
        if (isInOutage(divaca) && isInOutage(padriciano)) {
            pstOutput.setAndLogProcedure(DIVACA, 3);
        } else if (hasTargetFlow(divaca)) {
            divacaTargetFlowProcess(divaca, padriciano, pstOutput);
        } else {
            pstOutput.setAndLogProcedure(DIVACA, 1);
            pstOutput.setTotalTargetFlow(0);
            pstOutput.setTargetFlowDivacaPadriciano(0);
            pstOutput.setTargetFlowDivacaRedipuglia(0);
            if (!isInOutage(padriciano)) {
                Optional.ofNullable(padriciano.getPhaseTapChanger())
                        .ifPresent(changer -> changer.setTapPosition(0));
            }
        }
    }

    private void divacaTargetFlowProcess(final TwoWindingsTransformer divaca,
                                         final TwoWindingsTransformer padriciano,
                                         final PstOutput pstOutput) {
        // minus because in the XIIDM model :
        //      - regulation value follows load convention
        //      - target flow follows UCTE generator convention
        final double totalDivacaFlow = -getTargetFlow(divaca);
        pstOutput.setAndLogProcedure(DIVACA, 2);
        pstOutput.setTotalTargetFlow(totalDivacaFlow);

        final double divacaToPadriciano;
        final double divacaToRedipulgia;

        // at this point, at least one is not in outage
        if (isInOutage(padriciano)) {
            divacaToPadriciano = 0;
            divacaToRedipulgia = totalDivacaFlow;
            regulatePst(divaca, -divacaToRedipulgia);
            LOGGER.info("PST Padriciano is in outage");
        } else if (isInOutage(divaca)) {
            divacaToPadriciano = DIVACA_PADRICIANO_TARGET_FLOW;
            divacaToRedipulgia = 0;
            regulatePst(padriciano, divacaToPadriciano);
            LOGGER.info("PST Divača is in outage");
        } else {
            divacaToRedipulgia = totalDivacaFlow - DIVACA_PADRICIANO_TARGET_FLOW;
            divacaToPadriciano = DIVACA_PADRICIANO_TARGET_FLOW;
            regulatePst(padriciano, divacaToPadriciano);
            regulatePst(divaca, -divacaToRedipulgia);
        }

        pstOutput.setTargetFlowDivacaPadriciano(divacaToPadriciano);
        pstOutput.setTargetFlowDivacaRedipuglia(divacaToRedipulgia);
    }

    private void fillPstOutputsFromIgms(final MergingTask task,
                                        final Map<SpecialPst, String> pstIds,
                                        final PstOutput pstOutput,
                                        final LoadFlowParameters loadFlowParameters) {
        final Network slovenianGrid = task.getIgm(SI);

        runLoadFlowWithBalanceTypeCorrection(slovenianGrid, loadFlowRunnerSupplier, loadFlowParameters);
        applySolvedTapPositionAndSolvedSectionCount(slovenianGrid);

        pstOutput.getFlowDivacaPadriciano().setIgmFlowFromDanglingLine(DIVACA_PADRICIANO_DANGLING_LINE, slovenianGrid);
        pstOutput.getFlowDivacaRedipuglia().setIgmFlowFromDanglingLine(DIVACA_REDIPULGIA_DANGLING_LINE, slovenianGrid);

        forAllSpecialPst(pst -> pstOutput.setTapIgmFromId(pst, pstIds.get(pst), task.getIgm(pst.getCountry())));
    }

    private void fillPstOutputsFromCgm(final Network cgm,
                                       final Map<SpecialPst, String> pstIds,
                                       final PstOutput pstOutput,
                                       final LoadFlowParameters loadFlowParameters) {
        runLoadFlow(cgm, loadFlowRunnerSupplier, loadFlowParameters);
        applySolvedTapPositionAndSolvedSectionCount(cgm);

        pstOutput.getFlowDivacaPadriciano().setCgmFlowFromTieLine(DIVACA_PADRICIANO_LINE, cgm);
        pstOutput.getFlowDivacaRedipuglia().setCgmFlowFromTieLine(DIVACA_REDIPULGIA_LINE, cgm);

        forAllSpecialPst(pst -> pstOutput.setTapCgmFromId(pst, pstIds.get(pst), cgm));
    }

}

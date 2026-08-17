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
import com.powsybl.iidm.network.util.Networks;
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
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getBoundaryP;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getPstBranch;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getPstTieLine;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getTargetFlow;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.halveRegulationValue;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.hasTargetFlow;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.inconsistentTargetFlows;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.regulatePst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.setPstRegulating;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.LIENZ;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.NAUDERS1;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.NAUDERS2;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.PADRICIANO;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.forAllSpecialPst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.forAustrianPsts;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.toPstMap;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_FILE_AFTER_PST;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.PST_OUTPUT_FILE;
import static com.powsybl.iidm.network.Country.AT;
import static com.powsybl.iidm.network.Country.SI;

@Service
public class PstSpecialService {

    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;
    private static final Logger LOGGER = LoggerFactory.getLogger(PstSpecialService.class);

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
            // prepare all the data
            final LoadFlowParameters lfParameters = task.getConfigurations().getLoadFlowParameters();
            final SavedFile cgmFile = task.getArtifacts().getFile(BALANCED_CGM_FILE);
            final Network cgm = Network.read(cgmFile.getPath());
            final PstOutput pstOutput = new PstOutput();

            final Map<SpecialPst, TwoWindingsTransformer> pstsInIgms = toPstMap(pst -> getIgmPst(pst, task));
            final Map<SpecialPst, String> pstIds = toPstMap(
                    pst -> Optional.ofNullable(pstsInIgms.get(pst)).map(TwoWindingsTransformer::getId).orElse("")
            );

            fillPstOutputsFromIgms(task, pstIds, pstOutput, lfParameters);

            // handle IT-SI border Special PSTs
            final TwoWindingsTransformer divaca = cgm.getTwoWindingsTransformer(pstIds.get(DIVACA));
            final TwoWindingsTransformer padriciano = cgm.getTwoWindingsTransformer(pstIds.get(PADRICIANO));

            if (isInOutage(divaca) && isInOutage(padriciano)) {
                pstOutput.setAndLogProcedure(DIVACA, 3);
            } else if (hasTargetFlow(divaca)) {
                applyProcess2(divaca, padriciano, pstOutput);
            } else {
                applyProcess1(padriciano, pstOutput);
            }

            // handle AT Special PSTs
            final Network austria = task.getIgm(AT);
            applyLienzProcess(cgm.getTwoWindingsTransformer(pstIds.get(LIENZ)), pstOutput, austria);
            applyNaudersProcess(cgm.getTwoWindingsTransformer(pstIds.get(NAUDERS1)),
                                cgm.getTwoWindingsTransformer(pstIds.get(NAUDERS2)),
                                pstOutput,
                                austria);
            fillPstOutputsFromCgm(cgm, pstIds, pstOutput, lfParameters);

            // save results
            saveArtifactFile(PST_OUTPUT_FILE, pstOutput, task, configuration);
            saveArtifactNetwork(CGM_FILE_AFTER_PST, cgm, task, UCTE_FORMAT, configuration);

        } catch (final Exception e) {
            final String errorMessage = String.format("Error during fix PST special process for task '%d', cause : %s",
                                                      task.getId(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private TwoWindingsTransformer getIgmPst(final SpecialPst pst,
                                             final MergingTask task) {
        final TwoWindingsTransformer twoWindingsTransformer = task.getIgm(pst.getCountry())
                .getTwoWindingsTransformerStream()
                .filter(pst::matches)
                .findFirst()
                .orElse(null);

        if (twoWindingsTransformer == null) {
            LOGGER.warn("PST {} is not present in network of {}", pst.getFullName(), pst.getCountry().getName());
        }

        return twoWindingsTransformer;
    }

    private void applyProcess1(final TwoWindingsTransformer padriciano,
                               final PstOutput pstOutput) {
        pstOutput.setAndLogProcedure(DIVACA, 1);
        pstOutput.setTotalTargetFlowDivaca(0);
        pstOutput.setTargetFlowDivacaPadriciano(0);
        pstOutput.setTargetFlowDivacaRedipuglia(0);
        if (!isInOutage(padriciano)) {
            Optional.ofNullable(padriciano.getPhaseTapChanger())
                    .ifPresent(changer -> changer.setTapPosition(0));
        }
    }

    private void applyProcess2(final TwoWindingsTransformer divaca,
                               final TwoWindingsTransformer padriciano,
                               final PstOutput pstOutput) {
        // minus because in the XIIDM model :
        //      - regulation value follows load convention
        //      - target flow follows UCTE generator convention
        final double totalDivacaFlow = -getTargetFlow(divaca);
        pstOutput.setAndLogProcedure(DIVACA, 2);
        pstOutput.setTotalTargetFlowDivaca(totalDivacaFlow);

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
            LOGGER.info("PST Divaca is in outage");
        } else {
            divacaToRedipulgia = totalDivacaFlow - DIVACA_PADRICIANO_TARGET_FLOW;
            divacaToPadriciano = DIVACA_PADRICIANO_TARGET_FLOW;
            regulatePst(padriciano, divacaToPadriciano);
            regulatePst(divaca, -divacaToRedipulgia);
        }

        pstOutput.setTargetFlowDivacaPadriciano(divacaToPadriciano);
        pstOutput.setTargetFlowDivacaRedipuglia(divacaToRedipulgia);
    }

    private void applyLienzProcess(final TwoWindingsTransformer lienz,
                                   final PstOutput output,
                                   final Network austria) {

        Integer procedure = null;
        if (isInOutage(lienz)) { // outage in CGM
            procedure = 6;
        } else if (!hasTargetFlow(lienz)) {
            procedure = 4;
        } else {
            if (isInOutage(getPstBranch(LIENZ, austria))) { // outage in IGM
                LOGGER.warn("Lienz's tie line is inactive");
                setPstRegulating(lienz, false);
            } else {
                setPstRegulating(lienz, true);
                output.setTargetFlowLipst(-getTargetFlow(lienz));
                procedure = 5;
            }
        }

        Optional.ofNullable(procedure).ifPresent(nb -> output.setAndLogProcedure(LIENZ, nb));
    }

    private void applyNaudersProcess(final TwoWindingsTransformer nrpst21,
                                     final TwoWindingsTransformer nrpst22,
                                     final PstOutput output,
                                     final Network austria) {
        boolean pst21OutInCgm = isInOutage(nrpst21);
        boolean pst22OutInCgm = isInOutage(nrpst22);

        if (pst21OutInCgm && pst22OutInCgm) {
            output.setAndLogProcedure(NAUDERS1, 10);
        } else if (!hasTargetFlow(nrpst21) || !hasTargetFlow(nrpst22)) {
            output.setAndLogProcedure(NAUDERS1, 7);
        } else if (!pst21OutInCgm && !pst22OutInCgm) {
            // if out in IGM
            if (isInOutage(getPstBranch(NAUDERS1, austria)) || isInOutage(getPstBranch(NAUDERS2, austria))) {
                LOGGER.warn("At least one of Nauders's tie lines is inactive");
                setPstRegulating(nrpst21, false);
                setPstRegulating(nrpst22, false);
            }
            if (inconsistentTargetFlows(nrpst21, nrpst22)) {
                LOGGER.warn("Nauders PST: inconsistent target flows");
                setPstRegulating(nrpst21, false);
                setPstRegulating(nrpst22, false);
            } else {
                halveRegulationValue(nrpst21);
                halveRegulationValue(nrpst22);
                output.setAndLogProcedure(NAUDERS1, 8);
                output.setTargetFlowNrpst21(-getTargetFlow(nrpst21));
                output.setTargetFlowNrpst22(-getTargetFlow(nrpst22));
            }
        } else if (!pst21OutInCgm) {
            setPstRegulating(nrpst21, true);
            output.setAndLogProcedure(NAUDERS1, 9);
            output.setTargetFlowNrpst21(-getTargetFlow(nrpst21));
        } else {
            setPstRegulating(nrpst22, true);
            output.setAndLogProcedure(NAUDERS2, 9);
            output.setTargetFlowNrpst22(-getTargetFlow(nrpst22));

        }
    }

    private void fillPstOutputsFromIgms(final MergingTask task,
                                        final Map<SpecialPst, String> pstIds,
                                        final PstOutput pstOutput,
                                        final LoadFlowParameters loadFlowParameters) {
        final Network slovenianGrid = task.getIgm(SI);
        final Network austrianGrid = task.getIgm(AT);

        runLoadFlowWithBalanceTypeCorrection(slovenianGrid, loadFlowRunnerSupplier, loadFlowParameters);
        runLoadFlowWithBalanceTypeCorrection(austrianGrid, loadFlowRunnerSupplier, loadFlowParameters);
        Networks.applySolvedTapPositionAndSolvedSectionCount(slovenianGrid);
        Networks.applySolvedTapPositionAndSolvedSectionCount(austrianGrid);

        pstOutput.getFlowDivacaPadriciano().setFlowIGM(getBoundaryP(DIVACA_PADRICIANO_DANGLING_LINE, slovenianGrid));
        pstOutput.getFlowDivacaRedipuglia().setFlowIGM(getBoundaryP(DIVACA_REDIPULGIA_DANGLING_LINE, slovenianGrid));

        forAustrianPsts(pst -> pstOutput.getFlow(pst).setIgmFlowFromBranch(getPstBranch(pst, austrianGrid)));
        forAllSpecialPst(pst -> pstOutput.setTapIgmFromId(pst, pstIds.get(pst), task.getIgm(pst.getCountry())));
    }

    private void fillPstOutputsFromCgm(final Network cgm,
                                       final Map<SpecialPst, String> pstIds,
                                       final PstOutput pstOutput,
                                       final LoadFlowParameters loadFlowParameters) {
        runLoadFlow(cgm, loadFlowRunnerSupplier, loadFlowParameters);
        Networks.applySolvedTapPositionAndSolvedSectionCount(cgm);

        pstOutput.getFlowDivacaPadriciano().setCgmFlowFromBranch(getPstTieLine(DIVACA_PADRICIANO_LINE, cgm));
        pstOutput.getFlowDivacaRedipuglia().setCgmFlowFromBranch(getPstTieLine(DIVACA_REDIPULGIA_LINE, cgm));

        forAustrianPsts(pst -> pstOutput.getFlow(pst).setCgmFlowFromBranch(getPstBranch(pst, cgm)));
        forAllSpecialPst(pst -> pstOutput.setTapCgmFromId(pst, pstIds.get(pst), cgm));
    }

}

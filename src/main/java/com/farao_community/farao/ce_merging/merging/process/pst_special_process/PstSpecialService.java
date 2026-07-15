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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isInOutage;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getBoundaryP;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getPstBranch;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getPstTarget;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.getPstTieLine;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.hasTargetFlow;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.inconsistentTargetFlows;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.regulatePst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.PstUtils.setPstRegulating;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.DIVACA;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.LIENZ;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.NRPST21;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.NRPST22;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.PADRICIANO;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.forAllSpecialPst;
import static com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst.toPstMap;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.BALANCED_CGM_FILE;
import static com.powsybl.iidm.network.Country.AT;
import static com.powsybl.iidm.network.Country.SI;
import static com.powsybl.iidm.network.util.Networks.applySolvedTapPositionAndSolvedSectionCount;

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
    private static final int NEUTRAL_TAP = 0;
    private static final String PST_OUTPUT_FILENAME = "pstOutput.json";

    public PstSpecialService(CeMergingConfiguration configuration, Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.configuration = configuration;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }



    public void fixPst(final MergingTask task) {
        try {
            LoadFlowParameters loadFlowParametersForPst = task.getConfigurations().getLoadFlowParameters();
            final SavedFile cgmFile = task.getArtifacts().getFile(BALANCED_CGM_FILE);
            final Network cgm = Network.read(cgmFile.getPath());
            final PstOutput pstOutput = new PstOutput();

            final Map<SpecialPst, TwoWindingsTransformer> pstsInIgms = toPstMap(pst -> getTransformerFromIgm(pst, task));
            final Map<SpecialPst, String> pstIds = toPstMap(pst -> Optional.ofNullable(pstsInIgms.get(pst)).map(TwoWindingsTransformer::getId).orElse(""));

            fillPstOutputsFromIgms(task, pstIds, pstOutput, loadFlowParametersForPst);

            final TwoWindingsTransformer divaca = cgm.getTwoWindingsTransformer(pstIds.get(DIVACA));
            final TwoWindingsTransformer padriciano = cgm.getTwoWindingsTransformer(pstIds.get(PADRICIANO));

            if (isInOutage(divaca) && isInOutage(padriciano)) {
                pstOutput.setProcessNumberDivaca(3);
                LOGGER.info("Special PST Procedure 3 was applied, PST Divaca and Padriciano are in outage");
            } else if (hasTargetFlow(divaca)) {
                applyProcess2(divaca, padriciano, pstOutput);
            } else {
                applyProcess1(padriciano, pstOutput);
            }

            final Network austria = task.getIgm(AT);
            applyLienzProcess(cgm.getTwoWindingsTransformer(pstIds.get(LIENZ)), pstOutput, austria);
            applyNaudersProcess(cgm.getTwoWindingsTransformer(pstIds.get(NRPST21)),
                                cgm.getTwoWindingsTransformer(pstIds.get(NRPST22)),
                                pstOutput,
                                austria);
            fillPstOutputsFromCgm(cgm, pstIds, pstOutput, loadFlowParametersForPst);
            //TODO
            //savePstOutPutInArtifacts(pstOutput, taskEntity);
            //saveCgmInArtifacts(networkCgm, taskEntity);
        } catch (final Exception e) {
            final String errorMessage = String.format("Error during fix PST special process for task '%d', cause : %s",
                                                      task.getId(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private TwoWindingsTransformer getTransformerFromIgm(final SpecialPst pst, final MergingTask task) {
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
        pstOutput.setProcessNumberDivaca(1);
        pstOutput.setTotalTargetFlowDivaca(0);
        pstOutput.setTargetFlowDivacaPadriciano(0);
        pstOutput.setTargetFlowDivacaRedipuglia(0);
        if (!isInOutage(padriciano) && padriciano.getPhaseTapChanger() != null) {
            padriciano.getPhaseTapChanger().setTapPosition(NEUTRAL_TAP);
        }
        LOGGER.info("Special PST Procedure 1 was applied");
    }

    private void applyProcess2(final TwoWindingsTransformer divaca,
                               final TwoWindingsTransformer padriciano,
                               final PstOutput pstOutput) {
        // minus because in the XIIDM model :
        //      - regulation value follows load convention
        //      - target flow follow UCTE generator convention
        final double totalDivacaFlow = -getPstTarget(divaca);
        pstOutput.setProcessNumberDivaca(2);
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

        LOGGER.info("Special PST Procedure 2 was applied");
    }


    private void applyLienzProcess(final TwoWindingsTransformer lienz,
                                   final PstOutput output,
                                   final Network austria) {
        if (isInOutage(lienz)) { // outage in CGM
            output.setAppliedProcedureLipst(6);
            LOGGER.info("Lienz PST is in outage");
        } else if (!hasTargetFlow(lienz)) {
            output.setAppliedProcedureLipst(4);
            LOGGER.info("Lienz PST has no target flow");
        } else {
            if (isInOutage(getPstBranch(LIENZ, austria))) { // outage in IGM
                LOGGER.warn("Lienz's tie line is inactive");
                setPstRegulating(lienz, false);
            } else {
                setPstRegulating(lienz, true);
                output.setTargetFlowLipst(-getPstTarget(lienz));
                output.setAppliedProcedureLipst(5);
                LOGGER.info("Lienz target flow applied");
            }
        }
    }

    private void applyNaudersProcess(final TwoWindingsTransformer nrpst21,
                                     final TwoWindingsTransformer nrpst22,
                                     final PstOutput output,
                                     final Network austria) {
        boolean pst21OutInCgm = isInOutage(nrpst21);
        boolean pst22OutInCgm = isInOutage(nrpst22);

        if (pst21OutInCgm && pst22OutInCgm) {
            output.setAppliedProcedureNrpst(10);
            LOGGER.info("Both Nauders PSTs are outage");
        } else if (!hasTargetFlow(nrpst21) || !hasTargetFlow(nrpst22)) {
            output.setAppliedProcedureNrpst(7);
            LOGGER.info("No Nauders target flow defined");
        } else if (!pst21OutInCgm && !pst22OutInCgm) {
            // if out in IGM
            if (isInOutage(getPstBranch(NRPST21, austria)) || isInOutage(getPstBranch(NRPST22, austria))) {
                LOGGER.warn("At least one of Nauders tie lines is inactive");
                setPstRegulating(nrpst21, false);
                setPstRegulating(nrpst22, false);
            }
            if (inconsistentTargetFlows(nrpst21, nrpst22)) {
                LOGGER.warn("Nauders PST: inconsistent target flows");
                setPstRegulating(nrpst21, false);
                setPstRegulating(nrpst22, false);
            } else {
                setPstRegulating(nrpst21, true);
                setPstRegulating(nrpst22, true);
                output.setAppliedProcedureNrpst(8);
                output.setTargetFlowNrpst21(-getPstTarget(nrpst21));
                output.setTargetFlowNrpst22(-getPstTarget(nrpst22));
                LOGGER.info("Both Nauders PST are active");
            }
        } else if (!pst21OutInCgm) {
            setPstRegulating(nrpst21, true);
            output.setAppliedProcedureNrpst(9);
            output.setTargetFlowNrpst21(-getPstTarget(nrpst21));
            LOGGER.info("NRPST 21 is active");
        } else {
            setPstRegulating(nrpst22, true);
            output.setAppliedProcedureNrpst(9);
            output.setTargetFlowNrpst22(-getPstTarget(nrpst22));
            LOGGER.info("NRPST 22 is active");
        }


    }

    private void fillPstOutputsFromIgms(final MergingTask task,
                                        final Map<SpecialPst, String> pstIds,
                                        final PstOutput pstOutput,
                                        final LoadFlowParameters loadFlowParameters) {
        final Network slovenia = task.getIgm(SI);
        final Network austria = task.getIgm(AT);

        // TODO
        //LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection(slovenia, loadFlowRunnerSupplier, loadFlowParameters);
        //LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection(austria, loadFlowRunnerSupplier, loadFlowParameters);
        applySolvedTapPositionAndSolvedSectionCount(slovenia);
        applySolvedTapPositionAndSolvedSectionCount(austria);

        pstOutput.getFlowDivacaPadriciano().setFlowIGM(getBoundaryP(DIVACA_PADRICIANO_DANGLING_LINE, slovenia));
        pstOutput.getFlowDivacaRedipuglia().setFlowIGM(getBoundaryP(DIVACA_REDIPULGIA_DANGLING_LINE, slovenia));

        pstOutput.getFlowLipst().setFromIgmBranch(getPstBranch(LIENZ, austria));
        pstOutput.getFlowNrpst21().setFromIgmBranch(getPstBranch(NRPST21, austria));
        pstOutput.getFlowNrpst22().setFromIgmBranch(getPstBranch(NRPST22, austria));

        forAllSpecialPst(pst -> pstOutput.getTap(pst).setIgm(
            task.getIgm(pst.getCountry()).getTwoWindingsTransformer(pstIds.get(pst)))
        );

    }

    private void fillPstOutputsFromCgm(final Network cgm,
                                       final Map<SpecialPst, String> pstIds,
                                       final PstOutput pstOutput,
                                       final LoadFlowParameters loadFlowParameters) {
        //TODO
        //LoadFlowUtils.runLoadFlow(network, loadFlowRunnerSupplier, loadFlowParameters);
        applySolvedTapPositionAndSolvedSectionCount(cgm);

        pstOutput.getFlowDivacaPadriciano().setFromCgmBranch(getPstTieLine(DIVACA_PADRICIANO_LINE, cgm));
        pstOutput.getFlowDivacaRedipuglia().setFromCgmBranch(getPstTieLine(DIVACA_REDIPULGIA_LINE, cgm));

        pstOutput.getFlowLipst().setFromCgmBranch(getPstBranch(LIENZ, cgm));
        pstOutput.getFlowNrpst21().setFromCgmBranch(getPstBranch(NRPST21, cgm));
        pstOutput.getFlowNrpst22().setFromCgmBranch(getPstBranch(NRPST22, cgm));

        forAllSpecialPst(pst -> pstOutput.getTap(pst).setCgm(
            cgm.getTwoWindingsTransformer(pstIds.get(pst)))
        );
    }



   /* private void savePstOutPutInArtifacts(PstOutput pstOutput, MergingTask taskEntity) throws IOException {
        Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(taskEntity), PST_OUTPUT_FILENAME);
        byte[] file = JsonUtils.writeToBytes(PstOutput.class, pstOutput);
        Files.write(filePath, file);
        SavedFile savedItFile = new SavedFile(PST_OUTPUT_FILENAME, filePath.toString(), String.format("/tasks/%d/artifacts/pst-result", taskEntity.getTaskId()));
        taskEntity.getArtifacts().setPstOutputFile(savedItFile);
    }

    private void saveCgmInArtifacts(Network network, MergingTask taskEntity) {
        String fileName = generateCgmFileName(taskEntity);
        Path filePath = Paths.get(configuration.getArtifactsDirectoryPath(taskEntity), fileName);
        network.write("UCTE", null, filePath);
        SavedFile savedFile = new SavedFile(fileName, filePath.toString(), String.format("/tasks/%d/outputs/cgm", taskEntity.getTaskId()));
        taskEntity.getArtifacts().setCgmFileAfterPst(savedFile);
    }*/

    private String generateCgmFileName(MergingTask taskEntity) {
        // UCTE filename convention <yyyymmdd>_<HHMM>_<TY><w>_<cc><v>.uct
        ZonedDateTime targetDateInEuropeZone = taskEntity.getInputs().getTargetDate().atZoneSameInstant(ZoneId.of("Europe/Paris"));
        String dateAndTime = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").withLocale(Locale.FRANCE).format(targetDateInEuropeZone);
        String dayOfWeek = DateTimeFormatter.ofPattern("e").withLocale(Locale.FRANCE).format(targetDateInEuropeZone);
        return String.format("%s_2D%s_UX0_PST_APPLIED.uct", dateAndTime, dayOfWeek);
    }

}

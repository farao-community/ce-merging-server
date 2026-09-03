/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.SpecialPst;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.Flow;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.PstOutput;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import com.powsybl.iidm.network.Country;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.getNowDate;

public class MergingLogsBuilder {
    private static final String CORE_REPORT_TYPE_INFO = "CORE";
    private static final String OUT_CORE_REPORT_TYPE_INFO = "nonCORE";
    private static final String IDENTIFICATION = "A01";
    private static final String RECEIVER_ROLE = "A36";
    private static final String SENDER_ROLE = "A44";
    private static final String DOCUMENT_TYPE = "A18";
    private static final String ITALY_CODE = "IT";
    private static final String SLOVENIA_CODE = "SI";

    MergingLog buildMergingLog(final MergingTask task, final List<ReportInformationInRegion> reportInformationInRegionList, final ReferenceProgram referenceProgram, final PstOutput pstOutputs, final List<ReportInformationOutRegion> reportInformationsOutRegionList, final List<ReportCommonsInformation> tsoInformationsList, final List<AlegroReportInformation> alegroReportInformationsList) {
        final MergingLog mergingLog = new MergingLog();
        buildHeader(mergingLog, referenceProgram);
        mergingLog.setTimeSeries(buildTimeSeries(task, referenceProgram, reportInformationInRegionList, pstOutputs, reportInformationsOutRegionList, tsoInformationsList, alegroReportInformationsList));
        return mergingLog;
    }

    private MergingLog.TimeSeries buildTimeSeries(final MergingTask task, final ReferenceProgram referenceProgram, final List<ReportInformationInRegion> reportInformationInRegionList, final PstOutput pstOutputs, final List<ReportInformationOutRegion> reportInformationsOutRegionList, final List<ReportCommonsInformation> tsoInformationsList, final List<AlegroReportInformation> alegroReportInformationsList) {
        final MergingLog.TimeSeries timeSeries = new MergingLog.TimeSeries();
        final MergingLog.TimeSeries.TimeSeriesIdentification timeSeriesIdentification = new MergingLog.TimeSeries.TimeSeriesIdentification();
        timeSeriesIdentification.setV("1");
        timeSeries.setTimeSeriesIdentification(timeSeriesIdentification);
        timeSeries.setPeriod(buildPeriod(task, referenceProgram, reportInformationInRegionList, pstOutputs, reportInformationsOutRegionList, tsoInformationsList, alegroReportInformationsList));
        return timeSeries;
    }

    private MergingLog.TimeSeries.Period buildPeriod(final MergingTask task, final ReferenceProgram referenceProgram, final List<ReportInformationInRegion> reportInformationInRegionList, final PstOutput pstOutputs, final List<ReportInformationOutRegion> reportInformationsOutRegionList, final List<ReportCommonsInformation> tsoInformationsList, final List<AlegroReportInformation> alegroReportInformationsList) {
        final MergingLog.TimeSeries.Period period = new MergingLog.TimeSeries.Period();
        final MergingLog.TimeSeries.Period.Resolution resolution = new MergingLog.TimeSeries.Period.Resolution();
        resolution.setV(RESOLUTION);
        period.setResolution(resolution);
        final MergingLog.TimeSeries.Period.TimeInterval timeInterval = new MergingLog.TimeSeries.Period.TimeInterval();
        timeInterval.setV(referenceProgram.getDailyTimeInterval());
        period.setTimeInterval(timeInterval);
        final MergingLog.TimeSeries.Period.Interval interval = new MergingLog.TimeSeries.Period.Interval();
        interval.setMergingReport(computeMergingReport(task, reportInformationInRegionList, pstOutputs, reportInformationsOutRegionList, tsoInformationsList, alegroReportInformationsList));
        final MergingLog.TimeSeries.Period.Interval.Pos pos = new MergingLog.TimeSeries.Period.Interval.Pos();
        final OffsetDateTime periodStart = OffsetDateTime.parse(referenceProgram.getDailyTimeInterval().substring(0, 17), DateTimeFormatter.ISO_DATE_TIME);
        final OffsetDateTime periodEnd = OffsetDateTime.parse(referenceProgram.getDailyTimeInterval().substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
        final int position = OutputUtils.calculateTargetPosition(referenceProgram.getTargetDateTime(), periodStart, periodEnd);
        pos.setV(String.valueOf(position));
        interval.setPos(pos);
        period.getInterval().add(interval);
        return period;
    }

    private void buildHeader(final MergingLog mergingLog, final ReferenceProgram referenceProgram) {
        final MergingLog.DocumentIdentification documentIdentification = new MergingLog.DocumentIdentification();
        documentIdentification.setV(OutputUtils.getDocumentIdentificationDate(referenceProgram.getDailyTimeInterval()));
        mergingLog.setDocumentIdentification(documentIdentification);
        final MergingLog.DocumentVersion documentVersion = new MergingLog.DocumentVersion();
        documentVersion.setV((byte) 1);
        mergingLog.setDocumentVersion(documentVersion);
        final MergingLog.DocumentType documentType = new MergingLog.DocumentType();
        documentType.setV(DOCUMENT_TYPE);
        mergingLog.setDocumentType(documentType);
        final MergingLog.ProcessType processType = new MergingLog.ProcessType();
        processType.setV(IDENTIFICATION);
        mergingLog.setProcessType(processType);
        final MergingLog.SenderIdentification senderIdentification = new MergingLog.SenderIdentification();
        senderIdentification.setCodingScheme(IDENTIFICATION);
        senderIdentification.setV(SENDER_ID);
        mergingLog.setSenderIdentification(senderIdentification);
        final MergingLog.SenderRole senderRole = new MergingLog.SenderRole();
        senderRole.setV(SENDER_ROLE);
        mergingLog.setSenderRole(senderRole);
        final MergingLog.ReceiverIdentification receiverIdentification = new MergingLog.ReceiverIdentification();
        receiverIdentification.setCodingScheme(IDENTIFICATION);
        receiverIdentification.setV(RECEIVER_ID);
        mergingLog.setReceiverIdentification(receiverIdentification);
        final MergingLog.ReceiverRole receiverRole = new MergingLog.ReceiverRole();
        receiverRole.setV(RECEIVER_ROLE);
        mergingLog.setReceiverRole(receiverRole);
        final MergingLog.CreationDateTime creationDateTime = new MergingLog.CreationDateTime();
        creationDateTime.setV(getNowDate());
        mergingLog.setCreationDateTime(creationDateTime);
        final MergingLog.ReportTimeInterval reportTimeInterval = new MergingLog.ReportTimeInterval();
        reportTimeInterval.setV(referenceProgram.getDailyTimeInterval());
        mergingLog.setReportTimeInterval(reportTimeInterval);
        final MergingLog.Domain domain = new MergingLog.Domain();
        domain.setCodingScheme(IDENTIFICATION);
        domain.setV(CORE_REGION_ID);
        mergingLog.setDomain(domain);
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport computeMergingReport(final MergingTask task, final List<ReportInformationInRegion> reportInformationInRegionList, final PstOutput pstOutputs, final List<ReportInformationOutRegion> reportInformationsOutRegionList, final List<ReportCommonsInformation> tsoInformationsList, final List<AlegroReportInformation> alegroReportInformationList) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport mergingReport = new MergingLog.TimeSeries.Period.Interval.MergingReport();
        mergingReport.setMergeType(reportInformationInRegionList.get(0).mergeLoadflowType());
        mergingReport.setBCIactive(reportInformationInRegionList.get(0).bciActive());
        mergingReport.setBCIFeasibilityRangesExtended(reportInformationInRegionList.get(0).bciFeasibilityRangesExtended());
        reportInformationInRegionList.forEach(reportInformationInRegion ->
                mergingReport.getReport().add(computeCoreReport(task, reportInformationInRegion)));
        reportInformationsOutRegionList.forEach(reportInformationsOutRegion ->
                mergingReport.getReport().add(computeOutCoreReport(task, reportInformationsOutRegion)));
        tsoInformationsList.forEach(tsoInformations -> mergingReport.getReport().add(computeTsoReport(task, tsoInformations)));
        if (alegroReportInformationList != null) {
            alegroReportInformationList.forEach(alegroReportInformation -> {
                if (alegroReportInformation.globalNpTargetFinal() != null) {
                    mergingReport.getReport().add(computeAlegroReport(alegroReportInformation));
                }
            });
        }
        mergingReport.setSIITReport(computeSiItReport(task, pstOutputs));
        return mergingReport;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report computeCoreReport(final MergingTask task, final ReportInformationInRegion reportInformationInRegion) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        final ReportCommonsInformation reportCommonsInformation = reportInformationInRegion.reportCommonsInformation();
        report.setName(Country.valueOf(reportCommonsInformation.countryName()).getName());
        report.setTypeInfo(CORE_REPORT_TYPE_INFO);
        report.setId(task.getConfigurations().getRegionConfiguration().getAreasIn().get(reportCommonsInformation.countryName()));
        report.setBCI(getBci(reportInformationInRegion));
        report.setReferenceProgram(getReferenceProgramForRegion(reportInformationInRegion));
        report.setLoadFlow(computeLoadFlow(reportCommonsInformation));
        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report computeAlegroReport(final AlegroReportInformation alegroReportInformation) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        report.setName(alegroReportInformation.countryName());
        report.setTypeInfo(CORE_REPORT_TYPE_INFO);
        report.setId(getAlegroReportId(alegroReportInformation.countryName()));
        report.setReferenceProgram(getAlegroReferenceProgram(alegroReportInformation));
        report.setLoadFlow(getAlegroLoadFlow(alegroReportInformation));

        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram getAlegroReferenceProgram(final AlegroReportInformation alegroReportInformation) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram referenceProgram = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram();
        referenceProgram.setGlobalNPtargetInitial(alegroReportInformation.globalNpTargetInitial().shortValue());
        referenceProgram.setGlobalNPtargetFinal((short) Math.round(alegroReportInformation.globalNpTargetFinal()));
        return referenceProgram;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow getAlegroLoadFlow(final AlegroReportInformation information) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow loadFlow = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow();
        loadFlow.setCGM(getAlegroCgm(information));
        loadFlow.setIGM(getAlegroIgm(information));
        return loadFlow;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM getAlegroCgm(final AlegroReportInformation information) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM cgm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM();
        cgm.setGlobalBalance((int) Math.round(information.globalBalanceCgm()));
        cgm.setGeneration((int) Math.round(information.generationCgm()));
        cgm.setLoad((int) Math.round(information.loadCgm()));
        return cgm;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM getAlegroIgm(final AlegroReportInformation information) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM igm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM();
        igm.setGlobalBalance((int) Math.round(information.globalBalanceIgm()));
        igm.setGeneration((int) Math.round(information.generationIgm()));
        igm.setLoad((int) Math.round(information.loadIgm()));
        return igm;
    }

    private String getAlegroReportId(final String countryName) {
        if (countryName.equalsIgnoreCase(ALDE)) {
            return VIRTUAL_HUB_ALEGRO_DE_EIC;
        }
        return VIRTUAL_HUB_ALEGRO_BE_EIC;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram getReferenceProgramForRegion(final ReportInformationInRegion reportInformationInRegion) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram referenceProgram = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram();
        referenceProgram.setCoreNPtargetInitial(reportInformationInRegion.coreNpTargetInitial().shortValue());
        referenceProgram.setCoreNPtargetFinal(reportInformationInRegion.coreNpTargetFinal().shortValue());
        referenceProgram.setGlobalNPtargetInitial(reportInformationInRegion.globalNpTargetInitial().shortValue());
        referenceProgram.setGlobalNPtargetFinal(reportInformationInRegion.globalNpTargetFinal().shortValue());
        return referenceProgram;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.BCI getBci(final ReportInformationInRegion reportInformationInRegion) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.BCI bci = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.BCI();
        bci.setBCIapplied(reportInformationInRegion.bciApplied().toString());
        bci.setCOREInitialNPIGM(reportInformationInRegion.coreInitialNPIgm().shortValue());
        bci.setInitialMinNPShift(reportInformationInRegion.initialMinNpIgm().shortValue());
        bci.setInitialMaxNPShift(reportInformationInRegion.initialMaxNpIgm().shortValue());
        bci.setFinalMinNPShift(reportInformationInRegion.finalMinNpIgm().shortValue());
        bci.setFinalMaxNPShift(reportInformationInRegion.finalMaxNpIgm().shortValue());
        return bci;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report computeOutCoreReport(final MergingTask task, final ReportInformationOutRegion reportInformationsOutRegion) {
        MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        report.setName(Country.valueOf(reportInformationsOutRegion.reportCommonsInformation().countryName()).getName());
        report.setTypeInfo(OUT_CORE_REPORT_TYPE_INFO);
        report.setId(task.getConfigurations().getRegionConfiguration().getAreasAll().get(reportInformationsOutRegion.reportCommonsInformation().countryName()));
        report.setReferenceProgram(getReferenceProgramOutRegion(reportInformationsOutRegion));
        report.setLoadFlow(computeLoadFlow(reportInformationsOutRegion.reportCommonsInformation()));
        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram getReferenceProgramOutRegion(ReportInformationOutRegion reportInformationsOutRegion) {
        MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram referenceProgram = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram();
        referenceProgram.setGlobalNPtargetInitial(reportInformationsOutRegion.globalNpTargetInitial().shortValue());
        referenceProgram.setGlobalNPtargetFinal(reportInformationsOutRegion.globalNpTargetFinal().shortValue());
        return referenceProgram;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report computeTsoReport(final MergingTask task, final ReportCommonsInformation reportTsoInformations) {
        MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        final TsoInfos tso = task.getConfigurations().getRegionConfiguration().getGermanyZone().get(reportTsoInformations.countryName());
        report.setName(tso.getName());
        report.setTypeInfo(TSO);
        report.setId(tso.getEic());
        report.setLoadFlow(computeLoadFlow(reportTsoInformations));
        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow computeLoadFlow(final ReportCommonsInformation reportCommonsInformation) {
        MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow loadFlow = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow();
        loadFlow.setCGM(getCgm(reportCommonsInformation));
        loadFlow.setIGM(getIgm(reportCommonsInformation));
        return loadFlow;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM getIgm(ReportCommonsInformation reportCommonsInformation) {
        MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM igm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM();
        igm.setGeneration((int) reportCommonsInformation.generationIgm());
        igm.setLoad((int) reportCommonsInformation.loadIgm());
        igm.setGlobalBalance((int) reportCommonsInformation.globalBalanceIgm());
        return igm;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM getCgm(final ReportCommonsInformation reportCommonsInformation) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM cgm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM();
        cgm.setGeneration((int) reportCommonsInformation.generationCgm());
        cgm.setLoad((int) reportCommonsInformation.loadCgm());
        cgm.setGlobalBalance((int) reportCommonsInformation.globalBalanceCgm());
        return cgm;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport computeSiItReport(final MergingTask task, final PstOutput pstOutputs) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport siitReport = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport();
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.OutArea outArea = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.OutArea();
        final Map<String, String> areasAll = task.getConfigurations().getRegionConfiguration().getAreasAll();
        outArea.setCodingScheme(IDENTIFICATION);
        outArea.setV(areasAll.get(SLOVENIA_CODE));
        siitReport.setOutArea(outArea);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.InArea inArea = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.InArea();
        inArea.setCodingScheme(IDENTIFICATION);
        inArea.setV(areasAll.get(ITALY_CODE));
        siitReport.setInArea(inArea);
        siitReport.setAppliedProcedure((Integer.valueOf(pstOutputs.getProcessNumberDivaca())).byteValue());
        siitReport.setTargetFlow(getTargetFlow(pstOutputs));
        siitReport.setBeforeTargetFlowCGM(getBeforeTargetFlowCGM(pstOutputs));
        siitReport.setAfterTargetFlowCGM(getAfterTargetFlowCGM(pstOutputs));
        return siitReport;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.TargetFlow getTargetFlow(final PstOutput pstOutputs) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.TargetFlow targetFlow = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.TargetFlow();
        targetFlow.setTotalFlow((float) pstOutputs.getTotalTargetFlowDivaca());
        targetFlow.setFlowDivacaPadriciano((float) pstOutputs.getTargetFlowDivacaPadriciano());
        targetFlow.setFlowDivacaRedipuglia((float) pstOutputs.getTargetFlowDivacaRedipuglia());
        return targetFlow;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.BeforeTargetFlowCGM getBeforeTargetFlowCGM(final PstOutput pstOutputs) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.BeforeTargetFlowCGM beforeTargetFlowCGM = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.BeforeTargetFlowCGM();
        final Flow padriciano = pstOutputs.getFlowDivacaPadriciano();
        final Flow redipuglia = pstOutputs.getFlowDivacaRedipuglia();
        beforeTargetFlowCGM.setTotalFlow((float) (padriciano.getFlowIGM() + redipuglia.getFlowIGM()));
        beforeTargetFlowCGM.setFlowDivacaPadriciano((float) padriciano.getFlowIGM());
        beforeTargetFlowCGM.setFlowDivacaRedipuglia((float) redipuglia.getFlowIGM());
        beforeTargetFlowCGM.setTapsDivaca(pstOutputs.getTap(SpecialPst.DIVACA).getTapIGM());
        beforeTargetFlowCGM.setTapsPadriciano(pstOutputs.getTap(SpecialPst.PADRICIANO).getTapIGM());
        return beforeTargetFlowCGM;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.AfterTargetFlowCGM getAfterTargetFlowCGM(final PstOutput pstOutputs) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.AfterTargetFlowCGM afterTargetFlowCGM = new MergingLog.TimeSeries.Period.Interval.MergingReport.SIITReport.AfterTargetFlowCGM();
        final Flow padriciano = pstOutputs.getFlowDivacaPadriciano();
        final Flow redipuglia = pstOutputs.getFlowDivacaRedipuglia();
        afterTargetFlowCGM.setTotalFlow((float) (padriciano.getFlowCGM() + redipuglia.getFlowCGM()));
        afterTargetFlowCGM.setFlowDivacaPadriciano((float) padriciano.getFlowCGM());
        afterTargetFlowCGM.setFlowDivacaRedipuglia((float) redipuglia.getFlowCGM());
        afterTargetFlowCGM.setTapsPadriciano(pstOutputs.getTap(SpecialPst.PADRICIANO).getTapCGM());
        afterTargetFlowCGM.setTapsDivaca(pstOutputs.getTap(SpecialPst.DIVACA).getTapCGM());
        return afterTargetFlowCGM;
    }

}

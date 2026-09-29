
/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor;

import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.BCIActive;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.BCIDateTime;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.BalanceCGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.BalanceIGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.BaseCase;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.CWEFeasibilityRangeExport;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.CWEFeasibilityRangeImport;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.CWEInitialNetPositionIGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.CWERefProgCGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.CWERefProgIGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.ExportBCI;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.GenerationCGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.GenerationIGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.GlobalRefProgCGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.GlobalRefProgIGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.HubBCI;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.Pays;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.Reference;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.VerticalLoadCGM;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.VerticalLoadIGM;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CORE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_EIC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_EIC;
import static java.util.stream.Collectors.toMap;

public class BciExportBuilder {

    private static final List<String> TYPE_INFOS = Arrays.asList(CORE, TSO);
    private final MergingTask task;
    private final MergingLog mergingLog;

    public BciExportBuilder(final MergingTask task,
                            final MergingLog mergingLog) {
        this.task = task;
        this.mergingLog = mergingLog;
    }

    public ExportBCI buildBciExport() {
        final ExportBCI result = new ExportBCI();
        final Pays pays = new Pays();
        final List<MergingLog.TimeSeries.Period.Interval> mergeIntervals = mergingLog
                .getTimeSeries()
                .getPeriod()
                .getInterval();
        if (!mergeIntervals.isEmpty()) {
            final MergingLog.TimeSeries.Period.Interval.MergingReport mergingReport = mergeIntervals
                    .getFirst()
                    .getMergingReport();
            final BCIActive bciActive = new BCIActive();
            bciActive.setVal(mergingReport.isBCIactive());
            result.setBCIActive(bciActive);
            pays.getHubBCI().addAll(buildBciHubs(mergingReport));
        }
        final BCIDateTime bciDateTime = new BCIDateTime();
        bciDateTime.setVal(task.getInputs().getTargetDate().toString());
        result.setBCIDateTime(bciDateTime);
        result.setPays(pays);

        return result;
    }

    private List<HubBCI> buildBciHubs(final MergingLog.TimeSeries.Period.Interval.MergingReport mergingReport) {
        return mergingReport.getReport().stream()
                .filter(report -> TYPE_INFOS.contains(report.getTypeInfo()))
                .map(this::convertReport)
                .toList();
    }

    private String getCountryCode(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report) {
        return switch (report.getTypeInfo()) {
            case CORE -> getAreasAllMapInversed().get(report.getId());
            case TSO -> getTsoCodes().get(report.getName());
            default -> EMPTY;
        };
    }

    private Map<String, String> getAreasAllMapInversed() {
        return task.getConfigurations()
                .getRegionConfiguration()
                .getAreasAll()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getValue,
                               Map.Entry::getKey));
    }

    private Map<String, String> getTsoCodes() {
        return task.getConfigurations()
                .getRegionConfiguration()
                .getGermanyZone()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey,
                               e -> e.getValue().getName()));
    }

    private HubBCI convertReport(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report) {
        final HubBCI hubBCI = new HubBCI();
        hubBCI.setVal(getCountryCode(report));
        buildBaseCaseExport(report, hubBCI);
        final boolean isCe = report.getTypeInfo().equals(CORE);
        final boolean isAlegro = report.getId().equals(VIRTUAL_HUB_ALEGRO_BE_EIC) || report.getId().equals(VIRTUAL_HUB_ALEGRO_DE_EIC);
        if (isCe && !isAlegro) {
            buildReference(report, hubBCI);
        } else if (isCe) {
            hubBCI.setVal(report.getId());
            buildAlegroReference(report, hubBCI);
        }
        return hubBCI;
    }

    private void buildCommonData(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report,
                                 final Reference reference,
                                 final CWERefProgCGM cweRefProgCGM) {
        reference.setCWERefProgCGM(cweRefProgCGM);
        final GlobalRefProgCGM globalRefProgCGM = new GlobalRefProgCGM();
        globalRefProgCGM.setVal(BigInteger.valueOf(report.getReferenceProgram().getGlobalNPtargetFinal()));
        reference.setGlobalRefProgCGM(globalRefProgCGM);
        final GlobalRefProgIGM globalRefProgIGM = new GlobalRefProgIGM();
        globalRefProgIGM.setVal(BigInteger.valueOf(report.getReferenceProgram().getGlobalNPtargetInitial()));
        reference.setGlobalRefProgIGM(globalRefProgIGM);
        final CWEInitialNetPositionIGM cweInitialNetPositionIGM = new CWEInitialNetPositionIGM();
        reference.setCWEInitialNetPositionIGM(cweInitialNetPositionIGM);
    }

    private void buildAlegroReference(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report,
                                      final HubBCI hubBCI) {
        final Reference reference = new Reference();
        final CWEFeasibilityRangeExport cweFeasibilityRangeExport = new CWEFeasibilityRangeExport();
        reference.setCWEFeasibilityRangeExport(cweFeasibilityRangeExport);
        final CWEFeasibilityRangeImport cweFeasibilityRangeImport = new CWEFeasibilityRangeImport();
        reference.setCWEFeasibilityRangeImport(cweFeasibilityRangeImport);
        final CWERefProgIGM cweRefProgIGM = new CWERefProgIGM();
        reference.setCWERefProgIGM(cweRefProgIGM);
        final CWERefProgCGM cweRefProgCGM = new CWERefProgCGM();
        buildCommonData(report, reference, cweRefProgCGM);
        hubBCI.setReference(reference);
        final BCIActive bciActive = new BCIActive();
        hubBCI.setBCIActive(bciActive);
    }

    private void buildReference(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report,
                                final HubBCI hubBCI) {
        final Reference reference = new Reference();
        final CWEFeasibilityRangeExport cweFeasibilityRangeExport = new CWEFeasibilityRangeExport();
        cweFeasibilityRangeExport.setVal(BigInteger.valueOf(report.getBCI().getFinalMaxNPShift()));
        reference.setCWEFeasibilityRangeExport(cweFeasibilityRangeExport);
        final CWEFeasibilityRangeImport cweFeasibilityRangeImport = new CWEFeasibilityRangeImport();
        cweFeasibilityRangeImport.setVal(BigInteger.valueOf(report.getBCI().getInitialMinNPShift()));
        reference.setCWEFeasibilityRangeImport(cweFeasibilityRangeImport);
        final CWERefProgIGM cweRefProgIGM = new CWERefProgIGM();
        cweRefProgIGM.setVal(BigInteger.valueOf(report.getReferenceProgram().getCoreNPtargetInitial()));
        reference.setCWERefProgIGM(cweRefProgIGM);
        final CWERefProgCGM cweRefProgCGM = new CWERefProgCGM();
        cweRefProgCGM.setVal(BigInteger.valueOf(report.getReferenceProgram().getCoreNPtargetFinal()));
        buildCommonData(report, reference, cweRefProgCGM);
        hubBCI.setReference(reference);
        final BCIActive bciActive = new BCIActive();
        bciActive.setVal(Boolean.parseBoolean(report.getBCI().getBCIapplied()));
        hubBCI.setBCIActive(bciActive);
    }

    private void buildBaseCaseExport(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report,
                                     final HubBCI hubBCI) {
        final BaseCase baseCase = new BaseCase();
        final BalanceCGM balanceCGM = new BalanceCGM();
        balanceCGM.setVal(BigInteger.valueOf(report.getLoadFlow().getCGM().getGlobalBalance()));
        baseCase.setBalanceCGM(balanceCGM);
        final GenerationCGM generationCGM = new GenerationCGM();
        generationCGM.setVal(BigInteger.valueOf(report.getLoadFlow().getCGM().getGeneration()));
        baseCase.setGenerationCGM(generationCGM);
        final VerticalLoadCGM verticalLoadCGM = new VerticalLoadCGM();
        verticalLoadCGM.setVal(BigInteger.valueOf(report.getLoadFlow().getCGM().getLoad()));
        baseCase.setVerticalLoadCGM(verticalLoadCGM);
        final BalanceIGM balanceIGM = new BalanceIGM();
        balanceIGM.setVal(BigInteger.valueOf(report.getLoadFlow().getIGM().getGlobalBalance()));
        baseCase.setBalanceIGM(balanceIGM);
        final GenerationIGM generationIGM = new GenerationIGM();
        generationIGM.setVal(BigInteger.valueOf(report.getLoadFlow().getIGM().getGeneration()));
        baseCase.setGenerationIGM(generationIGM);
        final VerticalLoadIGM verticalLoadIGM = new VerticalLoadIGM();
        verticalLoadIGM.setVal(BigInteger.valueOf(report.getLoadFlow().getIGM().getLoad()));
        baseCase.setVerticalLoadIGM(verticalLoadIGM);
        hubBCI.setBaseCase(baseCase);
    }
}

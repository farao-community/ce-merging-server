/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.RegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.TsoInfos;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.ExportBCI;
import com.farao_community.farao.ce_merging.xsd.merging_supervisor.bci_nf.HubBCI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CORE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_EIC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_EIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BciExportBuilderTest {

    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.of(2026, 9, 29, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final String AREA_FR_EIC = "10YFR-RTE------C";
    private static final String AREA_BE_EIC = "10YBE----------2";

    private BciExportBuilder bciExportBuilder;
    private MergingTask mergingTask;

    @BeforeEach
    void setUp() {

        mergingTask = new MergingTask();
        mergingTask.getInputs().setTargetDate(TARGET_DATE);

        final RegionConfiguration regionConfiguration = new RegionConfiguration();
        final Map<String, String> areasIn = new HashMap<>();
        areasIn.put("FR", AREA_FR_EIC);
        areasIn.put("BE", AREA_BE_EIC);
        regionConfiguration.setAreasIn(areasIn);
        regionConfiguration.setAreasOut(Collections.emptyMap());

        final Map<String, TsoInfos> germanyZone = new HashMap<>();
        final TsoInfos tsoTransnet = new TsoInfos();
        tsoTransnet.setName("aGermanTso");
        germanyZone.put("aGermanTso", tsoTransnet);
        regionConfiguration.setGermanyZone(germanyZone);

        mergingTask.getConfigurations().setRegionConfiguration(regionConfiguration);
    }

    @Test
    void shouldBuildBciExportWhenIntervalsAreEmpty() {
        final MergingLog mergingLog = new MergingLog();
        final MergingLog.TimeSeries timeSeries = new MergingLog.TimeSeries();
        final MergingLog.TimeSeries.Period period = new MergingLog.TimeSeries.Period();
        timeSeries.setPeriod(period);
        mergingLog.setTimeSeries(timeSeries);

        bciExportBuilder = new BciExportBuilder(mergingTask, mergingLog);
        final ExportBCI exportBCI = bciExportBuilder.buildBciExport();

        assertNotNull(exportBCI);
        assertNotNull(exportBCI.getBCIDateTime());
        assertEquals(TARGET_DATE.toString(), exportBCI.getBCIDateTime().getVal());
        assertNull(exportBCI.getBCIActive());
        assertNotNull(exportBCI.getPays());
        assertTrue(exportBCI.getPays().getHubBCI().isEmpty());
    }

    @Test
    void shouldBuildBciExportWithStandardCeReport() {
        final MergingLog mergingLog = createMergingLog(true);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report ceReport = createCeReport(AREA_FR_EIC, "FR");
        mergingLog.getTimeSeries().getPeriod().getInterval().getFirst().getMergingReport().getReport().add(ceReport);

        bciExportBuilder = new BciExportBuilder(mergingTask, mergingLog);
        final ExportBCI exportBCI = bciExportBuilder.buildBciExport();

        assertNotNull(exportBCI);
        assertEquals(TARGET_DATE.toString(), exportBCI.getBCIDateTime().getVal());
        assertNotNull(exportBCI.getBCIActive());
        assertTrue(exportBCI.getBCIActive().isVal());

        final List<HubBCI> hubBCIList = exportBCI.getPays().getHubBCI();
        assertEquals(1, hubBCIList.size());

        final HubBCI hubBCI = hubBCIList.getFirst();
        assertEquals("FR", hubBCI.getVal());

        // BaseCase verification
        assertNotNull(hubBCI.getBaseCase());
        assertEquals(BigInteger.valueOf(25), hubBCI.getBaseCase().getBalanceCGM().getVal());
        assertEquals(BigInteger.valueOf(110), hubBCI.getBaseCase().getGenerationCGM().getVal());
        assertEquals(BigInteger.valueOf(85), hubBCI.getBaseCase().getVerticalLoadCGM().getVal());
        assertEquals(BigInteger.valueOf(20), hubBCI.getBaseCase().getBalanceIGM().getVal());
        assertEquals(BigInteger.valueOf(100), hubBCI.getBaseCase().getGenerationIGM().getVal());
        assertEquals(BigInteger.valueOf(80), hubBCI.getBaseCase().getVerticalLoadIGM().getVal());

        // Reference verification
        assertNotNull(hubBCI.getReference());
        assertEquals(BigInteger.valueOf(500), hubBCI.getReference().getCWEFeasibilityRangeExport().getVal());
        assertEquals(BigInteger.valueOf(-300), hubBCI.getReference().getCWEFeasibilityRangeImport().getVal());
        assertEquals(BigInteger.valueOf(1000), hubBCI.getReference().getCWERefProgIGM().getVal());
        assertEquals(BigInteger.valueOf(1050), hubBCI.getReference().getCWERefProgCGM().getVal());
        assertEquals(BigInteger.valueOf(2050), hubBCI.getReference().getGlobalRefProgCGM().getVal());
        assertEquals(BigInteger.valueOf(2000), hubBCI.getReference().getGlobalRefProgIGM().getVal());
        assertEquals(BigInteger.valueOf(0), hubBCI.getReference().getCWEInitialNetPositionIGM().getVal());

        // BCIActive verification
        assertNotNull(hubBCI.getBCIActive());
        assertTrue(hubBCI.getBCIActive().isVal());
    }

    @Test
    void shouldBuildBciExportWithAlegroCeReport() {
        final MergingLog mergingLog = createMergingLog(false);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report alegroReportBe = createAlegroReport(VIRTUAL_HUB_ALEGRO_BE_EIC);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report alegroReportDe = createAlegroReport(VIRTUAL_HUB_ALEGRO_DE_EIC);
        mergingLog.getTimeSeries().getPeriod().getInterval().getFirst().getMergingReport().getReport().add(alegroReportBe);
        mergingLog.getTimeSeries().getPeriod().getInterval().getFirst().getMergingReport().getReport().add(alegroReportDe);

        bciExportBuilder = new BciExportBuilder(mergingTask, mergingLog);
        final ExportBCI exportBCI = bciExportBuilder.buildBciExport();

        assertNotNull(exportBCI);
        assertNotNull(exportBCI.getBCIActive());
        assertFalse(exportBCI.getBCIActive().isVal());

        final List<HubBCI> hubBCIList = exportBCI.getPays().getHubBCI();
        assertEquals(2, hubBCIList.size());

        final HubBCI hubBe = hubBCIList.getFirst();
        assertEquals(VIRTUAL_HUB_ALEGRO_BE_EIC, hubBe.getVal());
        assertNotNull(hubBe.getBaseCase());
        assertNotNull(hubBe.getReference());
        assertNull(hubBe.getReference().getCWEFeasibilityRangeExport().getVal());
        assertNull(hubBe.getReference().getCWEFeasibilityRangeImport().getVal());
        assertNull(hubBe.getReference().getCWERefProgIGM().getVal());
        assertNull(hubBe.getReference().getCWERefProgCGM().getVal());
        assertEquals(BigInteger.valueOf(2050), hubBe.getReference().getGlobalRefProgCGM().getVal());
        assertEquals(BigInteger.valueOf(2000), hubBe.getReference().getGlobalRefProgIGM().getVal());
        assertNotNull(hubBe.getReference().getCWEInitialNetPositionIGM());
        assertNotNull(hubBe.getBCIActive());
        assertFalse(hubBe.getBCIActive().isVal());

        final HubBCI hubDe = hubBCIList.get(1);
        assertEquals(VIRTUAL_HUB_ALEGRO_DE_EIC, hubDe.getVal());
        assertNotNull(hubDe.getBaseCase());
        assertNotNull(hubDe.getReference());
    }

    @Test
    void shouldBuildBciExportWithTsoReport() {
        final MergingLog mergingLog = createMergingLog(true);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report tsoReport = createTsoReport("DE_TNG", "aGermanTso");
        mergingLog.getTimeSeries().getPeriod().getInterval().getFirst().getMergingReport().getReport().add(tsoReport);

        bciExportBuilder = new BciExportBuilder(mergingTask, mergingLog);
        final ExportBCI exportBCI = bciExportBuilder.buildBciExport();
        assertNotNull(exportBCI);
        final List<HubBCI> hubBCIList = exportBCI.getPays().getHubBCI();
        assertEquals(1, hubBCIList.size());

        final HubBCI hubBCI = hubBCIList.getFirst();
        assertEquals("aGermanTso", hubBCI.getVal());
        assertNotNull(hubBCI.getBaseCase());
        assertEquals(BigInteger.valueOf(25), hubBCI.getBaseCase().getBalanceCGM().getVal());
        assertNull(hubBCI.getReference());
        assertNull(hubBCI.getBCIActive());
    }

    @Test
    void shouldIgnoreReportsWithUnsupportedTypeInfo() {
        final MergingLog mergingLog = createMergingLog(true);
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report otherReport = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        otherReport.setTypeInfo("OTHER");
        otherReport.setId("OTHER_ID");
        otherReport.setName("OTHER_NAME");
        mergingLog.getTimeSeries().getPeriod().getInterval().getFirst().getMergingReport().getReport().add(otherReport);

        bciExportBuilder = new BciExportBuilder(mergingTask, mergingLog);
        final ExportBCI exportBCI = bciExportBuilder.buildBciExport();

        assertNotNull(exportBCI);
        assertTrue(exportBCI.getPays().getHubBCI().isEmpty());
    }

    private MergingLog createMergingLog(final boolean bciActive) {
        final MergingLog mergingLog = new MergingLog();
        final MergingLog.TimeSeries timeSeries = new MergingLog.TimeSeries();
        final MergingLog.TimeSeries.Period period = new MergingLog.TimeSeries.Period();
        final MergingLog.TimeSeries.Period.Interval interval = new MergingLog.TimeSeries.Period.Interval();
        final MergingLog.TimeSeries.Period.Interval.MergingReport mergingReport = new MergingLog.TimeSeries.Period.Interval.MergingReport();
        mergingReport.setBCIactive(bciActive);
        interval.setMergingReport(mergingReport);
        period.getInterval().add(interval);
        timeSeries.setPeriod(period);
        mergingLog.setTimeSeries(timeSeries);
        return mergingLog;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report createCeReport(final String id, final String name) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        report.setTypeInfo(CORE);
        report.setId(id);
        report.setName(name);

        populateLoadFlow(report);

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.BCI bci = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.BCI();
        bci.setFinalMaxNPShift((short) 500);
        bci.setInitialMinNPShift((short) -300);
        bci.setBCIapplied("true");
        report.setBCI(bci);

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram refProg = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram();
        refProg.setCoreNPtargetInitial((short) 1000);
        refProg.setCoreNPtargetFinal((short) 1050);
        refProg.setGlobalNPtargetInitial((short) 2000);
        refProg.setGlobalNPtargetFinal((short) 2050);
        report.setReferenceProgram(refProg);

        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report createAlegroReport(final String id) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        report.setTypeInfo(CORE);
        report.setId(id);
        report.setName(id);

        populateLoadFlow(report);

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram refProg = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.ReferenceProgram();
        refProg.setGlobalNPtargetInitial((short) 2000);
        refProg.setGlobalNPtargetFinal((short) 2050);
        report.setReferenceProgram(refProg);

        return report;
    }

    private MergingLog.TimeSeries.Period.Interval.MergingReport.Report createTsoReport(final String id, final String name) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report();
        report.setTypeInfo(TSO);
        report.setId(id);
        report.setName(name);

        populateLoadFlow(report);

        return report;
    }

    private void populateLoadFlow(final MergingLog.TimeSeries.Period.Interval.MergingReport.Report report) {
        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow loadFlow = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow();

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM igm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.IGM();
        igm.setGeneration(100);
        igm.setLoad(80);
        igm.setGlobalBalance(20);
        loadFlow.setIGM(igm);

        final MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM cgm = new MergingLog.TimeSeries.Period.Interval.MergingReport.Report.LoadFlow.CGM();
        cgm.setGeneration(110);
        cgm.setLoad(85);
        cgm.setGlobalBalance(25);
        loadFlow.setCGM(cgm);

        report.setLoadFlow(loadFlow);
    }
}

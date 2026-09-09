/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.dto.BecByBoundaryDto;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecByBoundary;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.BecCoefficients;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import com.farao_community.farao.ce_merging.global_grid_configurations.services.BECKeyConfigurationService;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import test_utils.TaskTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RefProgCalculationServiceTest {
    private static final String RESOURCES_PATH = "src/test/resources/refProg";
    private static final String CGM_NET_POSITION_FILE_NAME = "cgmNetPositions.json";
    private static final String PEVF_FILE_NAME = "20231103_0030_FO5_UX1.PEVF";
    private static final String FORECAST_REFERENCE_PROGRAM_FILE_NAME = "forecastReferenceProgram.json";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2023-11-03T00:30Z");

    private Map<String, Double> virtualHubsExchanges;
    private Map<String, BigInteger> coreExchangeResults;
    private Map<String, Double> nonCoreExchangeResults;
    private Map<String, PublicationDocument.PublicationTimeSeries> indexedTimeSeries;

    @Autowired
    BECKeyConfigurationService becKeyConfigurationService;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    RefProgCalculationService refProgCalculationService;

    MergingTask task = new MergingTask();

    @BeforeEach
    void setUp() throws Exception {
        initCoreMergingTaskEntity();
        virtualHubsExchanges = initVirtualHubsExchanges();
        coreExchangeResults = initCoreExchangeResults();
        nonCoreExchangeResults = initNonCoreExchanges();
    }

    @Test
    void computeRefProg() {
        refProgCalculationService.computeRefProg(task);
        final PublicationDocument refProgResult = JaxbUtils.readFromPath(PublicationDocument.class, task.getOutputs().getRefProg().getPath());

        assertNotNull(refProgResult);
        assertFalse(refProgResult.getPublicationTimeSeries().isEmpty());

        indexedTimeSeries = indexTimeSeries(refProgResult);

        checkCoreExchanges();
        checkNonCoreExchanges();
        checkVirtualHubsExchanges();
    }

    private Map<String, PublicationDocument.PublicationTimeSeries> indexTimeSeries(final PublicationDocument refProgResult) {
        return refProgResult.getPublicationTimeSeries()
                .stream()
                .collect(Collectors.toUnmodifiableMap(ts -> ts.getTimeSeriesIdentification().getV(), ts -> ts, (a, b) -> a));
    }

    private void checkCoreExchanges() {
        coreExchangeResults.forEach((tsId, val) -> assertEquals(val, getPublicationTimeSeriesByTimeSeriesIdentification(tsId).getPeriod().getInterval().getFirst().getQty().getV()));
    }

    private void checkNonCoreExchanges() {
        nonCoreExchangeResults.forEach((tsId, val) -> assertEquals(roundAndConvertToBigInteger(val), getPublicationTimeSeriesByTimeSeriesIdentification(tsId).getPeriod().getInterval().getFirst().getQty().getV()));
    }

    private void checkVirtualHubsExchanges() {
        final PublicationDocument.PublicationTimeSeries timeSeries1 = getPublicationTimeSeriesByTimeSeriesIdentification("NL-UK_BritNed");
        assertEquals(computeVirtualHubsExpectedFlow("XGR_MA1N"), timeSeries1.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries2 = getPublicationTimeSeriesByTimeSeriesIdentification("BE-UK_Nemolink");
        assertEquals(computeVirtualHubsExpectedFlow("XBE_GB1B"), timeSeries2.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries3 = getPublicationTimeSeriesByTimeSeriesIdentification("FR-UK_IFA2000_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE11"), timeSeries3.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries4 = getPublicationTimeSeriesByTimeSeriesIdentification("FR-UK_IFA2000_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE13"), timeSeries4.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries5 = getPublicationTimeSeriesByTimeSeriesIdentification("NL-NO_NorNed");
        assertEquals(computeVirtualHubsExpectedFlow("XEE_FE1N"), timeSeries5.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries6 = getPublicationTimeSeriesByTimeSeriesIdentification("NL-DK1_COBRA");
        assertEquals(computeVirtualHubsExpectedFlow("XED_EE1N"), timeSeries6.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries7 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-NL_COBRA");
        assertEquals(computeVirtualHubsExpectedFlow("XED_EE1D"), timeSeries7.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries8 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-DK2_Kontek");
        assertEquals(computeVirtualHubsExpectedFlow("XBW_BJ1D"), timeSeries8.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries9 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-SE_Baltic");
        assertEquals(computeVirtualHubsExpectedFlow("D2HWKR1D"), timeSeries9.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries10 = getPublicationTimeSeriesByTimeSeriesIdentification("PL-SE_SwePol");
        assertEquals(computeVirtualHubsExpectedFlow("XSL_SW11"), timeSeries10.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries11 = getPublicationTimeSeriesByTimeSeriesIdentification("PL-LT_LitPol1");
        assertEquals(computeVirtualHubsExpectedFlow("XEL_AL11"), timeSeries11.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries12 = getPublicationTimeSeriesByTimeSeriesIdentification("PL-LT_LitPol2");
        assertEquals(computeVirtualHubsExpectedFlow("XEL_AL12"), timeSeries12.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries13 = getPublicationTimeSeriesByTimeSeriesIdentification("PL-UA_Dobrotwor");
        assertEquals(computeVirtualHubsExpectedFlow("XZA_DO21"), timeSeries13.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries14 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-SE_Kontiskan1");
        assertEquals(computeVirtualHubsExpectedFlow("XVH_L11K"), timeSeries14.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries15 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-SE_Kontiskan2");
        assertEquals(computeVirtualHubsExpectedFlow("XVH_L21K"), timeSeries15.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries16 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-DK2_GreatBelt");
        assertEquals(computeVirtualHubsExpectedFlow("XFG_HK11"), timeSeries16.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries17 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-NO_Skagerrak1");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K13K"), timeSeries17.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries18 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-NO_Skagerrak2");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K23K"), timeSeries18.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries19 = getPublicationTimeSeriesByTimeSeriesIdentification("IT-GR_GrIt");
        assertEquals(computeVirtualHubsExpectedFlow("XAR_GA1I"), timeSeries19.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries20 = getPublicationTimeSeriesByTimeSeriesIdentification("GR-IT_GrIt");
        assertEquals(computeVirtualHubsExpectedFlow("XAR_GA1G"), timeSeries20.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries21 = getPublicationTimeSeriesByTimeSeriesIdentification("IT-ME_MONITA1");
        assertEquals(computeVirtualHubsExpectedFlow("XCEPR120"), timeSeries21.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries22 = getPublicationTimeSeriesByTimeSeriesIdentification("ME-IT_MONITA1");
        assertEquals(computeVirtualHubsExpectedFlow("XKOTR120"), timeSeries22.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries23 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-NO_Nordlink_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XWI_ET11"), timeSeries23.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries24 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-NO_Nordlink_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XWI_ET12"), timeSeries24.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries25 = getPublicationTimeSeriesByTimeSeriesIdentification("FR-UK_IFA2");
        assertEquals(computeVirtualHubsExpectedFlow("XTO_CH11"), timeSeries25.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries26 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-NO_Skagerrak3");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K31K"), timeSeries26.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries27 = getPublicationTimeSeriesByTimeSeriesIdentification("DK1-NO_Skagerrak4");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K41K"), timeSeries27.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries28 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-DK2_CGS");
        assertEquals(computeVirtualHubsExpectedFlow("D8BWW_25"), timeSeries28.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries29 = getPublicationTimeSeriesByTimeSeriesIdentification("FR-UK_Eleclink");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE15"), timeSeries29.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries30 = getPublicationTimeSeriesByTimeSeriesIdentification("BE-BE_AL");
        assertEquals(computeVirtualHubsExpectedFlow("XLI_OB1B"), timeSeries30.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries31 = getPublicationTimeSeriesByTimeSeriesIdentification("DE-DE_AL");
        assertEquals(computeVirtualHubsExpectedFlow("XLI_OB1A"), timeSeries31.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries32 = getPublicationTimeSeriesByTimeSeriesIdentification("IT-ME_MONITA2");
        assertEquals(computeVirtualHubsExpectedFlow("XCEPR220"), timeSeries32.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries33 = getPublicationTimeSeriesByTimeSeriesIdentification("ME-IT_MONITA2");
        assertEquals(computeVirtualHubsExpectedFlow("XKOTR220"), timeSeries33.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries34 = getPublicationTimeSeriesByTimeSeriesIdentification("ES-MA_ESMA_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XTA_FA11"), timeSeries34.getPeriod().getInterval().getFirst().getQty().getV());

        final PublicationDocument.PublicationTimeSeries timeSeries35 = getPublicationTimeSeriesByTimeSeriesIdentification("ES-MA_ESMA_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XTA_FA12"), timeSeries35.getPeriod().getInterval().getFirst().getQty().getV());
    }

    private PublicationDocument.PublicationTimeSeries getPublicationTimeSeriesByTimeSeriesIdentification(final String timeSeriesIdentification) {
        PublicationDocument.PublicationTimeSeries pub =  indexedTimeSeries.get(timeSeriesIdentification);
        if (pub == null) {
            throw new CeMergingException("Unable to find timeserie " + timeSeriesIdentification + " in refProg result");
        }
        return pub;
    }

    private BigInteger computeVirtualHubsExpectedFlow(String nodeName) {
        return roundAndConvertToBigInteger(virtualHubsExchanges.get(nodeName));
    }

    private static BigInteger roundAndConvertToBigInteger(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }

    private Map<String, Double> initVirtualHubsExchanges() {
        Map<String, Double> initVirtualHubsExchanges = new HashMap<>();
        initVirtualHubsExchanges.put("XGR_MA1N", -1031.0);
        initVirtualHubsExchanges.put("XBE_GB1B", 243.0);
        initVirtualHubsExchanges.put("XMA_SE11", 751.919);
        initVirtualHubsExchanges.put("XMA_SE13", 760.918);
        initVirtualHubsExchanges.put("XEE_FE1N", 0.0);
        initVirtualHubsExchanges.put("XED_EE1N", 0.0);
        initVirtualHubsExchanges.put("XED_EE1D", 0.0);
        initVirtualHubsExchanges.put("XBW_BJ1D", 0.0);
        initVirtualHubsExchanges.put("D2HWKR1D", 0.0);
        initVirtualHubsExchanges.put("XSL_SW11", -392.5);
        initVirtualHubsExchanges.put("XEL_AL11", 0.0);
        initVirtualHubsExchanges.put("XEL_AL12", 28.0);
        initVirtualHubsExchanges.put("XZA_DO21", 0.0);
        initVirtualHubsExchanges.put("XVH_L11K", 369.4);
        initVirtualHubsExchanges.put("XVH_L21K", 371.9);
        initVirtualHubsExchanges.put("XFG_HK11", -588.0);
        initVirtualHubsExchanges.put("XTJ_K13K", 251.2);
        initVirtualHubsExchanges.put("XTJ_K23K", 251.2);
        initVirtualHubsExchanges.put("XAR_GA1I", 0.0);
        initVirtualHubsExchanges.put("XAR_GA1G", 0.0);
        initVirtualHubsExchanges.put("XCEPR120", 0.0);
        initVirtualHubsExchanges.put("XKOTR120", 0.0);
        initVirtualHubsExchanges.put("XWI_ET11", 0.0);
        initVirtualHubsExchanges.put("XWI_ET12", 0.0);
        initVirtualHubsExchanges.put("XTO_CH11", 1029.21);
        initVirtualHubsExchanges.put("XTJ_K31K", 501.2);
        initVirtualHubsExchanges.put("XTJ_K41K", 683.8);
        initVirtualHubsExchanges.put("D8BWW_25", -296.33636474609375);
        initVirtualHubsExchanges.put("XMA_SE15", 1028.7);
        initVirtualHubsExchanges.put("XLI_OB1B", 3.0);
        initVirtualHubsExchanges.put("XLI_OB1A", -3.0);
        initVirtualHubsExchanges.put("XCEPR220", -519.8);
        initVirtualHubsExchanges.put("XKOTR220", 530.0);
        initVirtualHubsExchanges.put("XTA_FA11", 581.9);
        initVirtualHubsExchanges.put("XTA_FA12", -258.7);
        return Map.copyOf(initVirtualHubsExchanges);
    }

    private Map<String, BigInteger> initCoreExchangeResults() {
        final Map<String, BigInteger> initCoreExchangeResults = new HashMap<>();
        initCoreExchangeResults.put("BE-NL", BigInteger.valueOf(161));
        initCoreExchangeResults.put("FR-BE", BigInteger.valueOf(1850));
        initCoreExchangeResults.put("DE-NL", BigInteger.valueOf(-1366));
        initCoreExchangeResults.put("FR-DE", BigInteger.valueOf(3378));
        initCoreExchangeResults.put("DE-PL", BigInteger.valueOf(1295));
        initCoreExchangeResults.put("DE-CZ", BigInteger.valueOf(1191));
        initCoreExchangeResults.put("DE-AT", BigInteger.valueOf(2329));
        initCoreExchangeResults.put("PL-CZ", BigInteger.valueOf(-104));
        initCoreExchangeResults.put("PL-SK", BigInteger.valueOf(478));
        initCoreExchangeResults.put("AT-CZ", BigInteger.valueOf(-1138));
        initCoreExchangeResults.put("AT-SI", BigInteger.valueOf(975));
        initCoreExchangeResults.put("HR-SI", BigInteger.valueOf(-81));
        initCoreExchangeResults.put("AT-HU", BigInteger.valueOf(762));
        initCoreExchangeResults.put("CZ-SK", BigInteger.valueOf(581));
        initCoreExchangeResults.put("SK-HU", BigInteger.valueOf(1318));
        initCoreExchangeResults.put("HR-HU", BigInteger.valueOf(-294));
        initCoreExchangeResults.put("RO-HU", BigInteger.valueOf(-503));
        return Map.copyOf(initCoreExchangeResults);
    }

    private Map<String, Double> initNonCoreExchanges() {
        final Map<String, Double> initNonCoreExchangeResults = new HashMap<>();
        initNonCoreExchangeResults.put("BA-HR", 0.0);
        initNonCoreExchangeResults.put("BG-TR", -80.0);
        initNonCoreExchangeResults.put("CH-AT", -350.0);
        initNonCoreExchangeResults.put("CH-DE", -920.0);
        initNonCoreExchangeResults.put("CH-FR", -1400.0);
        initNonCoreExchangeResults.put("CH-IT", 1500.0);
        initNonCoreExchangeResults.put("DE-DK", 0.0);
        initNonCoreExchangeResults.put("ES-PT", 999.0);
        initNonCoreExchangeResults.put("FR-ES", 0.0);
        initNonCoreExchangeResults.put("FR-IT", 1300.0);
        initNonCoreExchangeResults.put("GR-AL", -120.0);
        initNonCoreExchangeResults.put("GR-BG", -470.0);
        initNonCoreExchangeResults.put("GR-MK", -74.0);
        initNonCoreExchangeResults.put("GR-TR", -10.0);
        initNonCoreExchangeResults.put("IT-AT", -112.0);
        initNonCoreExchangeResults.put("IT-SI", -254.0);
        initNonCoreExchangeResults.put("ME-AL", 40.0);
        initNonCoreExchangeResults.put("ME-BA", 0.0);
        initNonCoreExchangeResults.put("MK-BG", -130.0);
        initNonCoreExchangeResults.put("MK-KS", 45.0);
        initNonCoreExchangeResults.put("RO-BG", 300.0);
        initNonCoreExchangeResults.put("RS-BA", 0.0);
        initNonCoreExchangeResults.put("RS-BG", 0.0);
        initNonCoreExchangeResults.put("RS-HR", 0.0);
        initNonCoreExchangeResults.put("RS-HU", 0.0);
        initNonCoreExchangeResults.put("RS-ME", 0.0);
        initNonCoreExchangeResults.put("RS-MK", 0.0);
        initNonCoreExchangeResults.put("RS-RO", 0.0);
        initNonCoreExchangeResults.put("RS-KS", 0.0);
        initNonCoreExchangeResults.put("UA-HU", 0.0);
        initNonCoreExchangeResults.put("UA-RO", 0.0);
        initNonCoreExchangeResults.put("UA-SK", 0.0);
        initNonCoreExchangeResults.put("KS-AL", 70.0);
        initNonCoreExchangeResults.put("KS-ME", -20.0);
        return Map.copyOf(initNonCoreExchangeResults);
    }

    private void initCoreMergingTaskEntity() throws Exception {
        TaskTestUtils.setTaskDefaultConfigurations(task);
        task.setId(1L);
        initArtifacts();
        initInputs();
        initBecByBoundary();

        Files.createDirectories(Paths.get(configuration.getInputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
    }

    private void initArtifacts() {
        final Artifacts artifacts = new Artifacts();

        final SavedFile cgmNetPositionsFile = new SavedFile(CGM_NET_POSITION_FILE_NAME, RESOURCES_PATH + "/" + CGM_NET_POSITION_FILE_NAME, "mock");
        final SavedFile forecastReferenceProgram = new SavedFile(FORECAST_REFERENCE_PROGRAM_FILE_NAME, RESOURCES_PATH + "/" + FORECAST_REFERENCE_PROGRAM_FILE_NAME, "mock");

        artifacts.putFile(ArtifactType.CGM_NET_POSITIONS_FILE, cgmNetPositionsFile);
        artifacts.putFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE, forecastReferenceProgram);

        task.setArtifacts(artifacts);
    }

    private void initInputs() {
        final Inputs inputs = new Inputs();

        final SavedFile pevfFile = new SavedFile(PEVF_FILE_NAME, RESOURCES_PATH + "/" + PEVF_FILE_NAME, "mock");

        inputs.setNetPositionForecast(pevfFile);
        inputs.setTargetDate(TARGET_DATE);

        task.setInputs(inputs);
    }

    private void initBecByBoundary() throws Exception {
        final List<BecByBoundaryDto> becByBoundaryDtos = becKeyConfigurationService.getConfiguration(TARGET_DATE).getBecByBoundaries();
        final List<BecByBoundary> becByBoundaryList = becByBoundaryDtos.stream()
                .map(becByBoundaryDto -> {
                    Border border = new Border(becByBoundaryDto.getBorder().getOutArea(), becByBoundaryDto.getBorder().getInArea());
                    List<BecCoefficients> becCoefficients = becByBoundaryDto.getCoefficientByCountry().stream()
                            .map(becCoefficientsDto -> new BecCoefficients(becCoefficientsDto.getCountryCode(), becCoefficientsDto.getCoefficient()))
                            .toList();
                    return new BecByBoundary(border, becCoefficients);
                })
                .toList();

        task.getConfigurations().setBecMatrixConfig(becByBoundaryList);
    }
}

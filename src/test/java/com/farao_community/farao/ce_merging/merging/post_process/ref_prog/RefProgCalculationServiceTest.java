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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */

@SpringBootTest
class RefProgCalculationServiceTest {
    private static final String RESOURCES_PATH = "src/test/resources/refProg";
    private static final String CGM_NET_POSITION_FILE_NAME = "cgmNetPositions.json";
    private static final String PEVF_FILE_NAME = "20231103_0030_FO5_UX1.PEVF";
    private static final String FORECAST_REFERENCE_PROGRAM_FILE_NAME = "forecastReferenceProgram.json";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2023-11-03T00:30Z");

    private static final Map<String, Double> GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS = new HashMap<>();
    private static final Map<String, Double> AC_NET_POSITION_OUT_COUNTRY = new HashMap<>();
    private static final Map<String, Double> VIRTUAL_HUBS_EXCHANGES = new HashMap<>();

    private static final double FORECAST_NET_POSITION_BA_HR = 0.0;
    private static final double FORECAST_NET_POSITION_BG_TR = -80.0;
    private static final double FORECAST_NET_POSITION_CH_AT = -350.0;
    private static final double FORECAST_NET_POSITION_CH_DE = -920.0;
    private static final double FORECAST_NET_POSITION_CH_FR = -1400.0;
    private static final double FORECAST_NET_POSITION_CH_IT = 1500.0;
    private static final double FORECAST_NET_POSITION_DE_DK = 0.0;
    private static final double FORECAST_NET_POSITION_ES_PT = 999.0;
    private static final double FORECAST_NET_POSITION_FR_ES = 0.0;
    private static final double FORECAST_NET_POSITION_FR_IT = 1300.0;
    private static final double FORECAST_NET_POSITION_GR_AL = -120.0;
    private static final double FORECAST_NET_POSITION_GR_BG = -470.0;
    private static final double FORECAST_NET_POSITION_GR_MK = -74.0;
    private static final double FORECAST_NET_POSITION_GR_TR = -10.0;
    private static final double FORECAST_NET_POSITION_IT_AT = -112.0;
    private static final double FORECAST_NET_POSITION_IT_SI = -254.0;
    private static final double FORECAST_NET_POSITION_ME_AL = 40.0;
    private static final double FORECAST_NET_POSITION_ME_BA = 0.0;
    private static final double FORECAST_NET_POSITION_MK_BG = -130.0;
    private static final double FORECAST_NET_POSITION_MK_KS = 45.0;
    private static final double FORECAST_NET_POSITION_RO_BG = 300.0;
    private static final double FORECAST_NET_POSITION_RS_BA = 0.0;
    private static final double FORECAST_NET_POSITION_RS_BG = 0.0;
    private static final double FORECAST_NET_POSITION_RS_HR = 0.0;
    private static final double FORECAST_NET_POSITION_RS_HU = 0.0;
    private static final double FORECAST_NET_POSITION_RS_ME = 0.0;
    private static final double FORECAST_NET_POSITION_RS_MK = 0.0;
    private static final double FORECAST_NET_POSITION_RS_RO = 0.0;
    private static final double FORECAST_NET_POSITION_RS_KS = 0.0;
    private static final double FORECAST_NET_POSITION_UA_HU = 0.0;
    private static final double FORECAST_NET_POSITION_UA_RO = 0.0;
    private static final double FORECAST_NET_POSITION_UA_SK = 0.0;
    private static final double FORECAST_NET_POSITION_KS_AL = 70.0;
    private static final double FORECAST_NET_POSITION_KS_ME = -20.0;

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
        initGlobalNetPositionWithoutVirtualHubs();
        initAcNetPositionOutCountry();
        initVirtualHubsExchanges();
    }

    @Test
    void computeRefProg() {
        refProgCalculationService.computeRefProg(task);
        PublicationDocument refProgResult = JaxbUtils.readFromPath(PublicationDocument.class, task.getOutputs().getRefProg().getPath());

        assertNotNull(refProgResult);
        assertFalse(refProgResult.getPublicationTimeSeries().isEmpty());

        checkCoreExchanges(refProgResult);
        checkNonCoreExchanges(refProgResult);
        checkVirtualHubsExchanges(refProgResult);
    }

    private void checkCoreExchanges(PublicationDocument refProgResult) {

        PublicationDocument.PublicationTimeSeries timeSeriesBeNl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "BE-NL");
        BigInteger expectedFlow1 = BigInteger.valueOf(161);
        assertEquals(expectedFlow1, timeSeriesBeNl.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesFrBe = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-BE");
        BigInteger expectedFlow2 = BigInteger.valueOf(1850);
        assertEquals(expectedFlow2, timeSeriesFrBe.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesDeNl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-NL");
        BigInteger expectedFlow3 = BigInteger.valueOf(-1366);
        assertEquals(expectedFlow3, timeSeriesDeNl.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesFrDe = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-DE");
        BigInteger expectedFlow4 = BigInteger.valueOf(3378);
        assertEquals(expectedFlow4, timeSeriesFrDe.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesDePl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-PL");
        BigInteger expectedFlow5 = BigInteger.valueOf(1295);
        assertEquals(expectedFlow5, timeSeriesDePl.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesDeCz = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-CZ");
        BigInteger expectedFlow6 = BigInteger.valueOf(1191);
        assertEquals(expectedFlow6, timeSeriesDeCz.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesDeAt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-AT");
        BigInteger expectedFlow7 = BigInteger.valueOf(2329);
        assertEquals(expectedFlow7, timeSeriesDeAt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesPlCz = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-CZ");
        BigInteger expectedFlow8 = BigInteger.valueOf(-104);
        assertEquals(expectedFlow8, timeSeriesPlCz.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesPlSk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-SK");
        BigInteger expectedFlow9 = BigInteger.valueOf(478);
        assertEquals(expectedFlow9, timeSeriesPlSk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesAtCz = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "AT-CZ");
        BigInteger expectedFlow10 = BigInteger.valueOf(-1138);
        assertEquals(expectedFlow10, timeSeriesAtCz.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesAtSi = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "AT-SI");
        BigInteger expectedFlow11 = BigInteger.valueOf(975);
        assertEquals(expectedFlow11, timeSeriesAtSi.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesHrSi = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "HR-SI");
        BigInteger expectedFlow12 = BigInteger.valueOf(-81);
        assertEquals(expectedFlow12, timeSeriesHrSi.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesAtHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "AT-HU");
        BigInteger expectedFlow13 = BigInteger.valueOf(762);
        assertEquals(expectedFlow13, timeSeriesAtHu.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesCzSk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "CZ-SK");
        BigInteger expectedFlow14 = BigInteger.valueOf(581);
        assertEquals(expectedFlow14, timeSeriesCzSk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesSkHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "SK-HU");
        BigInteger expectedFlow15 = BigInteger.valueOf(1318);
        assertEquals(expectedFlow15, timeSeriesSkHu.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesHrHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "HR-HU");
        BigInteger expectedFlow16 = BigInteger.valueOf(-294);
        assertEquals(expectedFlow16, timeSeriesHrHu.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRoHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RO-HU");
        BigInteger expectedFlow17 = BigInteger.valueOf(-503);
        assertEquals(expectedFlow17, timeSeriesRoHu.getPeriod().getInterval().getFirst().getQty().getV());
    }

    private void checkNonCoreExchanges(PublicationDocument refProgResult) {
        PublicationDocument.PublicationTimeSeries timeSeriesDeDk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-DK");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_DE_DK), timeSeriesDeDk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesUaSk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "UA-SK");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_UA_SK), timeSeriesUaSk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesUaHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "UA-HU");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_UA_HU), timeSeriesUaHu.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesUaRo = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "UA-RO");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_UA_RO), timeSeriesUaRo.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesChDe = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "CH-DE");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_CH_DE), timeSeriesChDe.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesChAt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "CH-AT");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_CH_AT), timeSeriesChAt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesChFr = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "CH-FR");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_CH_FR), timeSeriesChFr.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesChIt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "CH-IT");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_CH_IT), timeSeriesChIt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesFrEs = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-ES");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_FR_ES), timeSeriesFrEs.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesEsPt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ES-PT");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_ES_PT), timeSeriesEsPt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesItSi = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "IT-SI");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_IT_SI), timeSeriesItSi.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsBg = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-BG");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_BG), timeSeriesRsBg.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsRo = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-RO");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_RO), timeSeriesRsRo.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRoBg = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RO-BG");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RO_BG), timeSeriesRoBg.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsHu = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-HU");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_HU), timeSeriesRsHu.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsHr = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-HR");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_HR), timeSeriesRsHr.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsBa = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-BA");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_BA), timeSeriesRsBa.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsMe = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-ME");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_ME), timeSeriesRsMe.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsMk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-MK");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_MK), timeSeriesRsMk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesMkBg = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "MK-BG");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_MK_BG), timeSeriesMkBg.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesGrMk = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "GR-MK");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_GR_MK), timeSeriesGrMk.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesGrBg = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "GR-BG");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_GR_BG), timeSeriesGrBg.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesGrTr = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "GR-TR");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_GR_TR), timeSeriesGrTr.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesGrAl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "GR-AL");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_GR_AL), timeSeriesGrAl.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesMeBa = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ME-BA");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_ME_BA), timeSeriesMeBa.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesMeAl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ME-AL");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_ME_AL), timeSeriesMeAl.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesBaHr = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "BA-HR");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_BA_HR), timeSeriesBaHr.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesFrIt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-IT");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_FR_IT), timeSeriesFrIt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesItAt = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "IT-AT");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_IT_AT), timeSeriesItAt.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesBgTr = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "BG-TR");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_BG_TR), timeSeriesBgTr.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesRsKs = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "RS-KS");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_RS_KS), timeSeriesRsKs.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesMkKs = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "MK-KS");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_MK_KS), timeSeriesMkKs.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesKsMe = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "KS-ME");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_KS_ME), timeSeriesKsMe.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeriesKsAl = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "KS-AL");
        assertEquals(roundAndConvertToBigInteger(FORECAST_NET_POSITION_KS_AL), timeSeriesKsAl.getPeriod().getInterval().getFirst().getQty().getV());
    }

    private void checkVirtualHubsExchanges(PublicationDocument refProgResult) {
        PublicationDocument.PublicationTimeSeries timeSeries1 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "NL-UK_BritNed");
        assertEquals(computeVirtualHubsExpectedFlow("XGR_MA1N"), timeSeries1.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries2 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "BE-UK_Nemolink");
        assertEquals(computeVirtualHubsExpectedFlow("XBE_GB1B"), timeSeries2.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries3 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-UK_IFA2000_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE11"), timeSeries3.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries4 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-UK_IFA2000_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE13"), timeSeries4.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries5 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "NL-NO_NorNed");
        assertEquals(computeVirtualHubsExpectedFlow("XEE_FE1N"), timeSeries5.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries6 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "NL-DK1_COBRA");
        assertEquals(computeVirtualHubsExpectedFlow("XED_EE1N"), timeSeries6.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries7 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-NL_COBRA");
        assertEquals(computeVirtualHubsExpectedFlow("XED_EE1D"), timeSeries7.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries8 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-DK2_Kontek");
        assertEquals(computeVirtualHubsExpectedFlow("XBW_BJ1D"), timeSeries8.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries9 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-SE_Baltic");
        assertEquals(computeVirtualHubsExpectedFlow("D2HWKR1D"), timeSeries9.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries10 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-SE_SwePol");
        assertEquals(computeVirtualHubsExpectedFlow("XSL_SW11"), timeSeries10.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries11 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-LT_LitPol1");
        assertEquals(computeVirtualHubsExpectedFlow("XEL_AL11"), timeSeries11.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries12 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-LT_LitPol2");
        assertEquals(computeVirtualHubsExpectedFlow("XEL_AL12"), timeSeries12.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries13 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "PL-UA_Dobrotwor");
        assertEquals(computeVirtualHubsExpectedFlow("XZA_DO21"), timeSeries13.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries14 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-SE_Kontiskan1");
        assertEquals(computeVirtualHubsExpectedFlow("XVH_L11K"), timeSeries14.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries15 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-SE_Kontiskan2");
        assertEquals(computeVirtualHubsExpectedFlow("XVH_L21K"), timeSeries15.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries16 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-DK2_GreatBelt");
        assertEquals(computeVirtualHubsExpectedFlow("XFG_HK11"), timeSeries16.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries17 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-NO_Skagerrak1");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K13K"), timeSeries17.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries18 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-NO_Skagerrak2");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K23K"), timeSeries18.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries19 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "IT-GR_GrIt");
        assertEquals(computeVirtualHubsExpectedFlow("XAR_GA1I"), timeSeries19.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries20 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "GR-IT_GrIt");
        assertEquals(computeVirtualHubsExpectedFlow("XAR_GA1G"), timeSeries20.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries21 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "IT-ME_MONITA1");
        assertEquals(computeVirtualHubsExpectedFlow("XCEPR120"), timeSeries21.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries22 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ME-IT_MONITA1");
        assertEquals(computeVirtualHubsExpectedFlow("XKOTR120"), timeSeries22.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries23 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-NO_Nordlink_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XWI_ET11"), timeSeries23.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries24 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-NO_Nordlink_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XWI_ET12"), timeSeries24.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries25 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-UK_IFA2");
        assertEquals(computeVirtualHubsExpectedFlow("XTO_CH11"), timeSeries25.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries26 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-NO_Skagerrak3");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K31K"), timeSeries26.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries27 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DK1-NO_Skagerrak4");
        assertEquals(computeVirtualHubsExpectedFlow("XTJ_K41K"), timeSeries27.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries28 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-DK2_CGS");
        assertEquals(computeVirtualHubsExpectedFlow("D8BWW_25"), timeSeries28.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries29 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "FR-UK_Eleclink");
        assertEquals(computeVirtualHubsExpectedFlow("XMA_SE15"), timeSeries29.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries30 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "BE-BE_AL");
        assertEquals(computeVirtualHubsExpectedFlow("XLI_OB1B"), timeSeries30.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries31 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "DE-DE_AL");
        assertEquals(computeVirtualHubsExpectedFlow("XLI_OB1A"), timeSeries31.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries32 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "IT-ME_MONITA2");
        assertEquals(computeVirtualHubsExpectedFlow("XCEPR220"), timeSeries32.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries33 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ME-IT_MONITA2");
        assertEquals(computeVirtualHubsExpectedFlow("XKOTR220"), timeSeries33.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries34 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ES-MA_ESMA_link1");
        assertEquals(computeVirtualHubsExpectedFlow("XTA_FA11"), timeSeries34.getPeriod().getInterval().getFirst().getQty().getV());

        PublicationDocument.PublicationTimeSeries timeSeries35 = getPublicationTimeSeriesByTimeSeriesIdentification(refProgResult, "ES-MA_ESMA_link2");
        assertEquals(computeVirtualHubsExpectedFlow("XTA_FA12"), timeSeries35.getPeriod().getInterval().getFirst().getQty().getV());
    }

    private static PublicationDocument.PublicationTimeSeries getPublicationTimeSeriesByTimeSeriesIdentification(PublicationDocument refProgResult, String timeSeriesIdentification) {
        return refProgResult.getPublicationTimeSeries().stream()
                .filter(publicationTimeSeries -> publicationTimeSeries.getTimeSeriesIdentification().getV().equals(timeSeriesIdentification))
                .findFirst()
                .orElseThrow(() -> new CeMergingException("Unable to find timeserie " + timeSeriesIdentification + " in refProg result"));
    }

    private static BigInteger computeVirtualHubsExpectedFlow(String nodeName) {
        return roundAndConvertToBigInteger(VIRTUAL_HUBS_EXCHANGES.get(nodeName));
    }

    private static BigInteger roundAndConvertToBigInteger(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toBigInteger();
    }

    private static void initGlobalNetPositionWithoutVirtualHubs() {
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("AT", -1025.725426260948);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("BE", -1322.773618710382);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("CZ", 1117.6163716316223);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("DE", 1360.3519231784344);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("FR", 8296.383736384256);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("HR", -7.692972660064697);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("HU", -914.1336374282837);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("NL", 1573.8933925628662);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("PL", -552.1899824142456);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("RO", 165.53404235839844);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("SI", -271.2135249376297);
        GLOBAL_NET_POSITION_WITHOUT_VIRTUAL_HUBS.put("SK", 628.4385685920715);
    }

    private static void initAcNetPositionOutCountry() {
        AC_NET_POSITION_OUT_COUNTRY.put("AT", -(FORECAST_NET_POSITION_CH_AT + FORECAST_NET_POSITION_IT_AT));
        AC_NET_POSITION_OUT_COUNTRY.put("BE", 0.0);
        AC_NET_POSITION_OUT_COUNTRY.put("CZ", 0.0);
        AC_NET_POSITION_OUT_COUNTRY.put("DE", FORECAST_NET_POSITION_DE_DK - FORECAST_NET_POSITION_CH_DE);
        AC_NET_POSITION_OUT_COUNTRY.put("FR", (FORECAST_NET_POSITION_FR_ES + FORECAST_NET_POSITION_FR_IT) - FORECAST_NET_POSITION_CH_FR);
        AC_NET_POSITION_OUT_COUNTRY.put("HR", -(FORECAST_NET_POSITION_BA_HR + FORECAST_NET_POSITION_RS_HR));
        AC_NET_POSITION_OUT_COUNTRY.put("HU", -(FORECAST_NET_POSITION_RS_HU + FORECAST_NET_POSITION_UA_HU));
        AC_NET_POSITION_OUT_COUNTRY.put("NL", 0.0);
        AC_NET_POSITION_OUT_COUNTRY.put("PL", 0.0);
        AC_NET_POSITION_OUT_COUNTRY.put("RO", FORECAST_NET_POSITION_RO_BG - (FORECAST_NET_POSITION_RS_RO + FORECAST_NET_POSITION_UA_RO));
        AC_NET_POSITION_OUT_COUNTRY.put("SI", -FORECAST_NET_POSITION_IT_SI);
        AC_NET_POSITION_OUT_COUNTRY.put("SK", -FORECAST_NET_POSITION_UA_SK);
    }

    private static void initVirtualHubsExchanges() {
        VIRTUAL_HUBS_EXCHANGES.put("XGR_MA1N", -1031.0);
        VIRTUAL_HUBS_EXCHANGES.put("XBE_GB1B", 243.0);
        VIRTUAL_HUBS_EXCHANGES.put("XMA_SE11", 751.919);
        VIRTUAL_HUBS_EXCHANGES.put("XMA_SE13", 760.918);
        VIRTUAL_HUBS_EXCHANGES.put("XEE_FE1N", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XED_EE1N", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XED_EE1D", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XBW_BJ1D", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("D2HWKR1D", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XSL_SW11", -392.5);
        VIRTUAL_HUBS_EXCHANGES.put("XEL_AL11", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XEL_AL12", 28.0);
        VIRTUAL_HUBS_EXCHANGES.put("XZA_DO21", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XVH_L11K", 369.4);
        VIRTUAL_HUBS_EXCHANGES.put("XVH_L21K", 371.9);
        VIRTUAL_HUBS_EXCHANGES.put("XFG_HK11", -588.0);
        VIRTUAL_HUBS_EXCHANGES.put("XTJ_K13K", 251.2);
        VIRTUAL_HUBS_EXCHANGES.put("XTJ_K23K", 251.2);
        VIRTUAL_HUBS_EXCHANGES.put("XAR_GA1I", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XAR_GA1G", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XCEPR120", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XKOTR120", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XWI_ET11", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XWI_ET12", 0.0);
        VIRTUAL_HUBS_EXCHANGES.put("XTO_CH11", 1029.21);
        VIRTUAL_HUBS_EXCHANGES.put("XTJ_K31K", 501.2);
        VIRTUAL_HUBS_EXCHANGES.put("XTJ_K41K", 683.8);
        VIRTUAL_HUBS_EXCHANGES.put("D8BWW_25", -296.33636474609375);
        VIRTUAL_HUBS_EXCHANGES.put("XMA_SE15", 1028.7);
        VIRTUAL_HUBS_EXCHANGES.put("XLI_OB1B", 3.0);
        VIRTUAL_HUBS_EXCHANGES.put("XLI_OB1A", -3.0);
        VIRTUAL_HUBS_EXCHANGES.put("XCEPR220", -519.8);
        VIRTUAL_HUBS_EXCHANGES.put("XKOTR220", 530.0);
        VIRTUAL_HUBS_EXCHANGES.put("XTA_FA11", 581.9);
        VIRTUAL_HUBS_EXCHANGES.put("XTA_FA12", -258.7);
    }

    private void initCoreMergingTaskEntity() throws Exception {
        TaskTestUtils.setTaskDefaultConfigurations(task);

        initArtifacts();
        initInputs();
        initBecByBoundary();

        Files.createDirectories(Paths.get(configuration.getInputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
    }

    private void initArtifacts() {
        Artifacts artifacts = new Artifacts();

        SavedFile cgmNetPositionsFile = new SavedFile(CGM_NET_POSITION_FILE_NAME, RESOURCES_PATH + "/" + CGM_NET_POSITION_FILE_NAME, "mock");
        SavedFile forecastReferenceProgram = new SavedFile(FORECAST_REFERENCE_PROGRAM_FILE_NAME, RESOURCES_PATH + "/" + FORECAST_REFERENCE_PROGRAM_FILE_NAME, "mock");

        artifacts.putFile(ArtifactType.CGM_NET_POSITIONS_FILE, cgmNetPositionsFile);
        artifacts.putFile(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE, forecastReferenceProgram);

        task.setArtifacts(artifacts);
    }

    private void initInputs() {
        Inputs inputs = new Inputs();

        SavedFile pevfFile = new SavedFile(PEVF_FILE_NAME, RESOURCES_PATH + "/" + PEVF_FILE_NAME, "mock");

        inputs.setNetPositionForecast(pevfFile);
        inputs.setTargetDate(TARGET_DATE);

        task.setInputs(inputs);
    }

    private void initBecByBoundary() throws Exception {
        List<BecByBoundaryDto> becByBoundaryDtos = becKeyConfigurationService.getConfiguration(TARGET_DATE).getBecByBoundaries();
        List<BecByBoundary> becByBoundaryList = becByBoundaryDtos.stream()
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

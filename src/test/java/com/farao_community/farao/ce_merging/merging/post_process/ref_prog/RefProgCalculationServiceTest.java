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
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import org.assertj.core.api.SoftAssertions;
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

import static java.math.BigInteger.valueOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test_utils.CeTestUtils.mockSavedFile;

@SpringBootTest
class RefProgCalculationServiceTest {
    private static final String BIDDING_ZONE_FILE_PATH = "src/test/resources/request-metadata/inputs/biddingzoneucte.xml";
    private static final String LF_PARAMS_FILE_PATH = "loadflow_parameters/ac-load-flow-parameters_all_components.json";
    private static final String CGM_FILE_PATH = "src/test/resources/targetNetPositionCalculation/20260123_0130_2D5_UX0_RECESSIVITY_APPLIED.uct";
    private static final String RESOURCES_PATH = "src/test/resources/refProg";
    private static final String CGM_NET_POSITION_FILE_NAME = "cgmNetPositions.json";
    private static final String PEVF_FILE_NAME = "20231103_0030_FO5_UX1.PEVF";
    private static final String FORECAST_REFERENCE_PROGRAM_FILE_NAME = "forecastReferenceProgram.json";
    private static final OffsetDateTime TARGET_DATE = OffsetDateTime.parse("2023-11-03T00:30Z");

    private Map<String, Double> virtualHubsExpectedResults;
    private Map<String, BigInteger> ceExpectedResults;
    private Map<String, Double> nonCeExpectedResults;
    private Map<String, PublicationDocument.PublicationTimeSeries> indexedTimeSeries;

    @Autowired
    BECKeyConfigurationService becKeyConfigurationService;

    @Autowired
    private CeMergingConfiguration configuration;

    @Autowired
    RefProgCalculationService refProgCalculationService;

    @Autowired
    MergingTaskRepository repository;

    MergingTask task = new MergingTask();

    @BeforeEach
    void setUp() throws Exception {
        initTask();
        virtualHubsExpectedResults = initVirtualHubsExchanges();
        ceExpectedResults = expectedCeResults();
        nonCeExpectedResults = expectedNonCeResults();
    }

    @Test
    void computeRefProg() {
        refProgCalculationService.computeRefProg(task);
        final PublicationDocument refProgResult = JaxbUtils.readFromPath(PublicationDocument.class, task.getOutputs().getRefProg().getPath());

        assertNotNull(refProgResult);
        assertFalse(refProgResult.getPublicationTimeSeries().isEmpty());

        indexedTimeSeries = indexTimeSeries(refProgResult);

        checkCeExchanges();
        checkNonCeExchanges();
        checkVirtualHubsExchanges();
    }

    private Map<String, PublicationDocument.PublicationTimeSeries> indexTimeSeries(final PublicationDocument refProgResult) {
        return refProgResult.getPublicationTimeSeries()
                .stream()
                .collect(Collectors.toUnmodifiableMap(ts -> ts.getTimeSeriesIdentification().getV(), ts -> ts, (a, b) -> a));
    }

    private void checkCeExchanges() {
        final SoftAssertions softly = new SoftAssertions();
        ceExpectedResults.forEach((serieName, expectedValue) ->
                                          softly.assertThat(getPublicationTimeSeriesByTimeSeriesIdentification(serieName).getPeriod().getInterval().getFirst().getQty().getV())
                                                  .isEqualTo(expectedValue));
        softly.assertAll();
    }

    private void checkNonCeExchanges() {
        final SoftAssertions softly = new SoftAssertions();
        nonCeExpectedResults.forEach((serieName, expectedValue) -> softly.assertThat(getPublicationTimeSeriesByTimeSeriesIdentification(serieName).getPeriod().getInterval().getFirst().getQty().getV())
                .isEqualTo(roundAndConvertToBigInteger(expectedValue)));
        softly.assertAll();
    }

    private void checkVirtualHubsExchanges() {
        final SoftAssertions softly = new SoftAssertions();
        assertVirtualHubFlowEqualsTimeSerieValue("XGR_MA1N", "NL-UK_BritNed", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XBE_GB1B", "BE-UK_Nemolink", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XMA_SE11", "FR-UK_IFA2000_link1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XMA_SE13", "FR-UK_IFA2000_link2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XEE_FE1N", "NL-NL_NO2_NorNed", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XED_EE1N", "NL-NL_DK1_COBRA", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XED_EE1D", "DK1-DK1_NL_COBRA", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XBW_BJ1D", "DE-DK2_Kontek", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("D2HWKR1D", "DE-DE_SE4_Baltic", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XSL_SW11", "PL-PL_SE4_SwePol", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XEL_AL11", "PL-LT_LitPol1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XEL_AL12", "PL-LT_LitPol2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XZA_DO21", "PL-UA_Dobrotwor", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XVH_L11K", "DK1-SE_Kontiskan1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XVH_L21K", "DK1-SE_Kontiskan2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XFG_HK11", "DK1-DK2_GreatBelt", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTJ_K13K", "DK1-NO_Skagerrak1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTJ_K23K", "DK1-NO_Skagerrak2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XAR_GA1I", "ISOU-GR_GrIt", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XAR_GA1G", "GR-IT_GrIt", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XCEPR120", "ICSO-ME_MONITA1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XKOTR120", "ME-IT_MONITA1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XWI_ET11", "DE-NO_Nordlink_link1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XWI_ET12", "DE-NO_Nordlink_link2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTO_CH11", "FR-UK_IFA2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTJ_K31K", "DK1-NO_Skagerrak3", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTJ_K41K", "DK1-NO_Skagerrak4", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("D8BWW_25", "DE-DK2_CGS", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XMA_SE15", "FR-UK_Eleclink", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XLI_OB1B", "BE-BE_AL", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XLI_OB1A", "DE-DE_AL", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XCEPR220", "ICSO-ME_MONITA2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XKOTR220", "ME-IT_MONITA2", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTA_FA11", "ES-MA_ESMA_link1", softly);
        assertVirtualHubFlowEqualsTimeSerieValue("XTA_FA12", "ES-MA_ESMA_link2", softly);
        softly.assertAll();
    }

    private void assertVirtualHubFlowEqualsTimeSerieValue(final String nodeName,
                                                          final String timeSerieName,
                                                          final SoftAssertions assertions) {
        final PublicationDocument.PublicationTimeSeries timeSeries = getPublicationTimeSeriesByTimeSeriesIdentification(timeSerieName);
        assertions.assertThat(computeVirtualHubsExpectedFlow(nodeName)).isEqualTo(timeSeries.getPeriod().getInterval().getFirst().getQty().getV());
    }

    private PublicationDocument.PublicationTimeSeries getPublicationTimeSeriesByTimeSeriesIdentification(final String timeSeriesIdentification) {
        PublicationDocument.PublicationTimeSeries pub = indexedTimeSeries.get(timeSeriesIdentification);
        if (pub == null) {
            throw new CeMergingException("Unable to find timeserie " + timeSeriesIdentification + " in refProg result");
        }
        return pub;
    }

    private BigInteger computeVirtualHubsExpectedFlow(String nodeName) {
        return roundAndConvertToBigInteger(virtualHubsExpectedResults.get(nodeName));
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

    private Map<String, BigInteger> expectedCeResults() {
        final Map<String, BigInteger> ceExchanges = new HashMap<>();
        ceExchanges.put("BE-NL", valueOf(-639));
        ceExchanges.put("FR-BE", valueOf(652));
        ceExchanges.put("DE-NL", valueOf(-962));
        ceExchanges.put("FR-DE", valueOf(975));
        ceExchanges.put("DE-PL", valueOf(357));
        ceExchanges.put("DE-CZ", valueOf(178));
        ceExchanges.put("DE-AT", valueOf(1014));
        ceExchanges.put("PL-CZ", valueOf(-179));
        ceExchanges.put("PL-SK", valueOf(13));
        ceExchanges.put("AT-CZ", valueOf(-836));
        ceExchanges.put("AT-SI", valueOf(282));
        ceExchanges.put("HR-SI", valueOf(43));
        ceExchanges.put("AT-HU", valueOf(217));
        ceExchanges.put("CZ-SK", valueOf(192));
        ceExchanges.put("SK-HU", valueOf(861));
        ceExchanges.put("HR-HU", valueOf(-22));
        ceExchanges.put("RO-HU", valueOf(-106));
        ceExchanges.put("FR-INOR", valueOf(2165));
        ceExchanges.put("CH-INOR", valueOf(332));
        ceExchanges.put("INOR-AT", valueOf(-175));
        ceExchanges.put("INOR-SI", valueOf(106));
        ceExchanges.put("CH-AT", valueOf(157));
        ceExchanges.put("CH-DE", valueOf(-857));
        ceExchanges.put("CH-FR", valueOf(-1833));
        return Map.copyOf(ceExchanges);
    }

    private Map<String, Double> expectedNonCeResults() {
        final Map<String, Double> nonCeExchanges = new HashMap<>();
        nonCeExchanges.put("BA-HR", 0.0);
        nonCeExchanges.put("BG-TR", -80.0);
        nonCeExchanges.put("DE-DK", 0.0);
        nonCeExchanges.put("ES-PT", 999.0);
        nonCeExchanges.put("FR-ES", 0.0);
        nonCeExchanges.put("GR-AL", -120.0);
        nonCeExchanges.put("GR-BG", -470.0);
        nonCeExchanges.put("GR-MK", -74.0);
        nonCeExchanges.put("GR-TR", -10.0);
        nonCeExchanges.put("ME-AL", 40.0);
        nonCeExchanges.put("ME-BA", 0.0);
        nonCeExchanges.put("MK-BG", -130.0);
        nonCeExchanges.put("MK-KS", 45.0);
        nonCeExchanges.put("RO-BG", 300.0);
        nonCeExchanges.put("RS-BA", 0.0);
        nonCeExchanges.put("RS-BG", 0.0);
        nonCeExchanges.put("RS-HR", 0.0);
        nonCeExchanges.put("RS-HU", 0.0);
        nonCeExchanges.put("RS-ME", 0.0);
        nonCeExchanges.put("RS-MK", 0.0);
        nonCeExchanges.put("RS-RO", 0.0);
        nonCeExchanges.put("RS-KS", 0.0);
        nonCeExchanges.put("UA-HU", 0.0);
        nonCeExchanges.put("UA-RO", 0.0);
        nonCeExchanges.put("UA-SK", 0.0);
        nonCeExchanges.put("KS-AL", 70.0);
        nonCeExchanges.put("KS-ME", -20.0);
        return Map.copyOf(nonCeExchanges);
    }

    private void initTask() throws Exception {
        TaskTestUtils.setTaskDefaultConfigurations(task);
        TaskTestUtils.setLoadflowParameters(task, LF_PARAMS_FILE_PATH);
        repository.save(task);
        initArtifacts();
        initInputs();
        initBecByBoundary();
        task.setOutputs(new Outputs());
        task.getOutputs().setCgm(mockSavedFile(CGM_FILE_PATH));

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

        inputs.setNetPositionForecast(mockSavedFile(RESOURCES_PATH + "/" + PEVF_FILE_NAME));
        inputs.setTargetDate(TARGET_DATE);
        inputs.setInternalBiddingZones(mockSavedFile(BIDDING_ZONE_FILE_PATH));

        task.setInputs(inputs);
    }

    private void initBecByBoundary() {
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

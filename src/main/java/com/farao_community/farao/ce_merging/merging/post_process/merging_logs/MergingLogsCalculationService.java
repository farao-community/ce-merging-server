/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.merging_logs;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceExchangeData;
import com.farao_community.farao.ce_merging.merging.process.base_case_improvement.data.inputs.ReferenceProgram;
import com.farao_community.farao.ce_merging.merging.process.final_cgm_result.FinalCgmResult;
import com.farao_community.farao.ce_merging.merging.process.pst_special_process.output.PstOutput;
import com.farao_community.farao.ce_merging.merging.process.target_net_positions.bci.JsonBciOutputStructure;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.merging.task.enums.OutputType;
import com.farao_community.farao.ce_merging.xsd.merging_logs.MergingLog;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;

@Service
public class MergingLogsCalculationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MergingLogsCalculationService.class);

    private final CeMergingConfiguration configuration;
    private final TsoInformationsService tsoInformationsService;
    private final MergingTaskRepository repository;
    private static final String GERMANY_CODE = "DE";
    private static final String BELGIUM_CODE = "BE";
    private static final AlegroReportParameters ALEGRO_BE = new AlegroReportParameters(BELGIUM_CODE, ALBE, VIRTUAL_HUB_ALEGRO_BE_NODE_NAME, VIRTUAL_HUB_ALEGRO_BE_EIC);
    private static final AlegroReportParameters ALEGRO_DE = new AlegroReportParameters(GERMANY_CODE, ALDE, VIRTUAL_HUB_ALEGRO_DE_NODE_NAME, VIRTUAL_HUB_ALEGRO_DE_EIC);

    public MergingLogsCalculationService(MergingTaskRepository repository, CeMergingConfiguration configuration, TsoInformationsService tsoInformationsService) {
        this.repository = repository;
        this.configuration = configuration;
        this.tsoInformationsService = tsoInformationsService;
    }

    public void computeMergingLogs(final MergingTask task) {
        try {
            final MergingLog mergingLog = buildMergingLog(task);
            saveMergingLogsFileInOutputs(mergingLog, task);
            repository.save(task);
        } catch (final Exception e) {
            final String errorMessage = String.format("Merging-logs computation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private MergingLog buildMergingLog(final MergingTask task) {
        MergingLog mergingLog;
        final JsonBciOutputStructure bciOutputs = JsonUtils.read(JsonBciOutputStructure.class, task.getArtifactPath(ArtifactType.BCI_OUTPUT_FILE));
        final PstOutput pstOutputs = JsonUtils.read(PstOutput.class, task.getArtifactPath(ArtifactType.PST_OUTPUT_FILE));
        final NetPositionsResults igmNetPositionsResults = JsonUtils.read(NetPositionsResults.class, task.getArtifactPath(ArtifactType.IGMS_NET_POSITIONS_FILE));
        final FinalCgmResult cgmResult = JsonUtils.read(FinalCgmResult.class, task.getArtifactPath(ArtifactType.CGM_NET_POSITIONS_FILE));
        final ReferenceProgram referenceProgram = JsonUtils.read(ReferenceProgram.class, task.getArtifactPath(ArtifactType.REFERENCE_PROGRAM_FORECAST_FILE));
        final String loadflowMode = cgmResult.getLoadFlowResults() != null ? cgmResult.getLoadFlowResults().getLoadflowMode() : AC;
        final NetPositionsResults cgmNetPositionsResults = cgmResult.getNetPositionsResults();
        final List<ReportInformationInRegion> reportInformationInRegionList = fillInRegionMergingReports(bciOutputs, cgmNetPositionsResults, igmNetPositionsResults, loadflowMode);
        final List<ReportInformationOutRegion> reportInformationsOutRegionList = fillOutRegionMergingReports(bciOutputs, cgmNetPositionsResults, igmNetPositionsResults);
        final List<ReportCommonsInformation> tsoInformationsList = tsoInformationsService.calculateTsoInformations(task);
        MergingLogsBuilder mergingLogsBuilder = new MergingLogsBuilder();
        if (task.getInputs().getMergingWithInternalHvdc()) {
            final List<AlegroReportInformation> alegroReportInformationsList = buildAlegroReports(referenceProgram, cgmNetPositionsResults, igmNetPositionsResults);
            mergingLog = mergingLogsBuilder.buildMergingLog(task, reportInformationInRegionList, referenceProgram, pstOutputs, reportInformationsOutRegionList, tsoInformationsList, alegroReportInformationsList);
        } else {
            mergingLog = mergingLogsBuilder.buildMergingLog(task, reportInformationInRegionList, referenceProgram, pstOutputs, reportInformationsOutRegionList, tsoInformationsList, null);
        }
        return mergingLog;
    }

    private List<AlegroReportInformation> buildAlegroReports(final ReferenceProgram referenceProgram, final NetPositionsResults cgmNetPositionsResults, final NetPositionsResults igmNetPositionsResults) {
        return List.of(
                buildAlegroReport(ALEGRO_BE, referenceProgram, cgmNetPositionsResults, igmNetPositionsResults),
                buildAlegroReport(ALEGRO_DE, referenceProgram, cgmNetPositionsResults, igmNetPositionsResults)
        );
    }

    private AlegroReportInformation buildAlegroReport(final AlegroReportParameters parameters, final ReferenceProgram referenceProgram, final NetPositionsResults cgmNetPositionsResults, final NetPositionsResults igmNetPositionsResults) {
        final double globalNpTargetInitial = getAlegroTargetInitialFlow(referenceProgram, parameters.virtualHubEic());
        final NetPositions cgmNetPositions = cgmNetPositionsResults.netPositionsByCountryMap().get(parameters.countryCode());
        if (cgmNetPositions == null) {
            LOGGER.warn("{} does not exist in net positions", parameters.countryCode());
            return AlegroReportInformation.unavailable(parameters.reportCountryName(), globalNpTargetInitial);
        }
        final Double cgmExchange = cgmNetPositions.getVirtualHubsExchanges().get(parameters.virtualHubNodeName());
        if (cgmExchange == null) {
            LOGGER.warn("{} does not exist in virtual hubs list", parameters.virtualHubNodeName());
            return AlegroReportInformation.unavailable(parameters.reportCountryName(), globalNpTargetInitial);
        }

        final NetPositions igmNetPositions = igmNetPositionsResults.netPositionsByCountryMap().get(parameters.countryCode());
        if (igmNetPositions == null) {
            LOGGER.warn("{} does not exist in net positions", parameters.countryCode());
            return AlegroReportInformation.unavailable(parameters.reportCountryName(), globalNpTargetInitial);
        }
        final Double igmExchange = igmNetPositions.getVirtualHubsExchanges().get(parameters.virtualHubNodeName());
        if (igmExchange == null) {
            LOGGER.warn("{} does not exist in virtual hubs list", parameters.virtualHubNodeName());
            return AlegroReportInformation.unavailable(parameters.reportCountryName(), globalNpTargetInitial);
        }

        return new AlegroReportInformation(parameters.reportCountryName(), globalNpTargetInitial, -cgmExchange, -igmExchange, -cgmExchange, 0.0, igmExchange, 0.0, cgmExchange
        );
    }

    List<ReportInformationInRegion> fillInRegionMergingReports(JsonBciOutputStructure bciOutputs, NetPositionsResults cgmNetPositionsResults, NetPositionsResults igmNetPositionsResults, String loadflowMode) {
        List<ReportInformationInRegion> reportInformationInRegionList = new ArrayList<>();
        bciOutputs.getJsonBciComputationResult().getBciResults().forEach((key, value) -> {
            final ReportCommonsInformation reportCommonsInformation = fillReportCommonInformation(key, cgmNetPositionsResults, igmNetPositionsResults);
            reportInformationInRegionList.add(new ReportInformationInRegion(
                    reportCommonsInformation,
                    bciOutputs.getJsonBciComputationResult().isBciActive(),
                    bciOutputs.getJsonBciComputationResult().isBciFeasibilityRangesExtended(),
                    loadflowMode,
                    value.getBciApplied(),
                    value.getJsonInRegionNetPositions().getInitial(),
                    value.getJsonInRegionNetPositions().getInitialMin(),
                    value.getJsonInRegionNetPositions().getInitialMax(),
                    value.getJsonInRegionNetPositions().getFinalMin(),
                    value.getJsonInRegionNetPositions().getFinalMax(),
                    value.getJsonInRegionNetPositions().getForecast(),
                    value.getJsonInRegionNetPositions().getTarget(),
                    value.getJsonGlobalNetPositions().getForecast(),
                    value.getJsonGlobalNetPositions().getTarget()
            ));
        });
        return reportInformationInRegionList;
    }

    List<ReportInformationOutRegion> fillOutRegionMergingReports(JsonBciOutputStructure bciOutputs, NetPositionsResults cgmNetPositionsResults, NetPositionsResults igmNetPositionsResults) {
        final List<ReportInformationOutRegion> reportInformationsOutRegionList = new ArrayList<>();
        bciOutputs.getJsonOutRegionResults().getGlobalForecastNetPositions().forEach((key, value) -> {
            final ReportCommonsInformation reportCommonsInformation = fillReportCommonInformation(key, cgmNetPositionsResults, igmNetPositionsResults);
            reportInformationsOutRegionList.add(new ReportInformationOutRegion(reportCommonsInformation, value, value));
        });
        return reportInformationsOutRegionList;
    }

    private ReportCommonsInformation fillReportCommonInformation(final String zoneId, final NetPositionsResults cgmNetPositionsResults, final NetPositionsResults igmNetPositionsResults) {
        double generationCgm = 0.;
        double loadCgm = 0.;
        double globalBalanceCgm = 0.;

        final NetPositions cgmNetPositions = cgmNetPositionsResults.netPositionsByCountryMap().get(zoneId);
        if (cgmNetPositions != null) {
            generationCgm = cgmNetPositions.getGenerationAndLoadQuantity().generation();
            loadCgm = cgmNetPositions.getGenerationAndLoadQuantity().load();
            globalBalanceCgm = cgmNetPositions.getGlobalNetPosition().getWithoutVirtualHubs();
        }
        double generationIgm = 0.;
        double loadIgm = 0.;
        double globalBalanceIgm = 0.;

        final NetPositions igmNetPositions = igmNetPositionsResults.netPositionsByCountryMap().get(zoneId);
        if (igmNetPositions != null) {
            generationIgm = igmNetPositions.getGenerationAndLoadQuantity().generation();
            loadIgm = igmNetPositions.getGenerationAndLoadQuantity().load();
            globalBalanceIgm = igmNetPositions.getGlobalNetPosition().getWithoutVirtualHubs();
        }
        return new ReportCommonsInformation(zoneId, generationIgm, loadIgm, globalBalanceIgm, generationCgm, loadCgm, globalBalanceCgm);
    }

    private void saveMergingLogsFileInOutputs(final MergingLog mergingLogResult, final MergingTask task) {
        final OutputType outputType = OutputType.MERGING_LOGS;
        final String fileName = outputType.getFileName(task.getInputs().getTargetDate());
        final String location = outputType.getLocation(task.getId());
        final SavedFile mergingLogsSavedFile = FileStorageUtils.save(
                configuration.getOutputsDirectoryPath(task),
                fileName,
                location,
                path -> writeMergingLogXml(mergingLogResult, path)
        );

        task.getOutputs().setMergingLogs(mergingLogsSavedFile);
    }

    private void writeMergingLogXml(final MergingLog mergingLogResult, final Path filePath) {
        try {
            final JAXBContext context = JAXBContext.newInstance(MergingLog.class);
            final Marshaller marshaller = context.createMarshaller();
            final DOMResult domResult = new DOMResult();
            marshaller.marshal(mergingLogResult, domResult);
            final TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            final Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(domResult.getNode()), new StreamResult(filePath.toFile()));
        } catch (final Exception e) {
            throw new CeMergingException(String.format("Cannot save merging logs file in the output directory"), e);
        }
    }

    private double getAlegroTargetInitialFlow(final ReferenceProgram referenceProgram, final String alegroCode) {
        return referenceProgram.getReferenceExchangeDataList()
                .stream()
                .filter(data -> alegroCode.equals(data.getAreaOutId()))
                .findFirst()
                .map(ReferenceExchangeData::getFlow)
                .orElseThrow(() -> new CeMergingException("No reference exchange found for Alegro code '" + alegroCode + "'"));
    }

    private record AlegroReportParameters(
            String countryCode,
            String reportCountryName,
            String virtualHubNodeName,
            String virtualHubEic) {
    }
}

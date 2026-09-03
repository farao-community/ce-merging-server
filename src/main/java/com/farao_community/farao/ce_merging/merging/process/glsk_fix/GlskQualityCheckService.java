/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeAdder;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.entsoe.commons.PowsyblEntsoeReportResourceBundle;
import com.powsybl.glsk.ucte.quality_check.GlskQualityProcessor;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.GregorianCalendar;
import java.util.function.Function;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_EIC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_EIC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_CODE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_CODE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.SENDER_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.RECEIVER_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_BASE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CORE_REGION_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;

@Service
public class GlskQualityCheckService {
    static final String LOAD = "Load";
    static final String GENERATOR = "Generator";
    static final String NODE_ID_KEY = "NodeId";
    private static final String GLSK = "GLSK";
    static final String TYPE_KEY = "Type";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskQualityCheckService.class);

    private final CeMergingConfiguration configuration;
    private final GlskFixService glskFixService;

    public GlskQualityCheckService(CeMergingConfiguration configuration, GlskFixService glskFixService) {
        this.configuration = configuration;
        this.glskFixService = glskFixService;
    }

    public void runQualityCheck(final MergingTask task) {
        final Inputs inputs = task.getInputs();
        final OffsetDateTime processTargetDate = inputs.getTargetDate();
        final SavedFile mergedFile = task.getArtifacts().getFile(ArtifactType.TGM_FILE_AFTER_RECESSIVITY);
        final SavedFile glskFile = inputs.getGenerationLoadShiftKeys();
        try (InputStream mergedFileIs = new FileInputStream(mergedFile.getPath());
             InputStream glskFileIs = new FileInputStream(glskFile.getPath())) {
            final byte[] glskBytes = IOUtils.toByteArray(glskFileIs);
            glskFixService.checkTimeInterval(glskBytes, inputs.getTargetDate());
            ReportNode rootReportNode = createRootReportNode();
            GlskQualityProcessor.process(mergedFile.getOriginalName(), mergedFileIs, new ByteArrayInputStream(glskBytes), processTargetDate.toInstant(), rootReportNode);
            final byte[] correctedGlsk = glskFixService.fixGlsk(glskBytes, rootReportNode, processTargetDate.toInstant());
            FileStorageUtils.saveArtifactFileWithWriter(
                    ArtifactType.GLSK_QUALITY_CORRECTED_FILE,
                    task,
                    configuration,
                    path -> {
                        try {
                            Files.write(path, correctedGlsk);
                        } catch (IOException e) {
                            throw new CeMergingException("Cannot write corrected GLSK artifact file", e);
                        }
                    }
            );
            if (Boolean.TRUE.equals(inputs.getMergingWithInternalHvdc())) {
                Network network = Network.read(mergedFile.getPath());
                rootReportNode = checkAlegroGlskSeries(network, glskBytes, processTargetDate, rootReportNode);
            }
            QualityCheckReport xmlReport = exportQualityReport(rootReportNode, processTargetDate);
            FileStorageUtils.saveArtifactFileWithWriter(
                    ArtifactType.GLSK_QUALITY_REPORT,
                    task,
                    configuration,
                    path -> JaxbUtils.writeToPath(QualityCheckReport.class, xmlReport, path)
            );

        } catch (final Exception e) {
            final String errorMessage = String.format("GLSK quality check failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private ReportNode createRootReportNode() {
        return ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME, PowsyblEntsoeReportResourceBundle.BASE_NAME)
                .withMessageTemplate("glsk.quality.report")
                .build();
    }

    ReportNode createReportWithoutAlegroReports(final ReportNode initialReporter) {
        //delete the log initially generated
        final ReportNode rootReportNode = ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME, PowsyblEntsoeReportResourceBundle.BASE_NAME)
                .withMessageTemplate(initialReporter.getMessageKey())
                .build();
        initialReporter.getChildren().stream()
                .filter(report -> !isAlegroReport(report))
                .forEach(reportNode -> {
                    ReportNodeAdder reportNodeAdder = rootReportNode.newReportNode()
                            .withMessageTemplate(reportNode.getMessageKey());

                    reportNode.getValues().forEach((key, value) -> reportNodeAdder.withTypedValue(key, value.toString(), ""));

                    reportNodeAdder.add();
                });
        return rootReportNode;
    }

    private boolean isAlegroReport(final ReportNode reportNode) {
        final Optional<TypedValue> reportNodeValue = reportNode.getValue(TSO);
        if (reportNodeValue.isEmpty()) {
            return false;
        }
        final String tsoEic = reportNodeValue.get().toString();
        return VIRTUAL_HUB_ALEGRO_BE_EIC.equals(tsoEic) || VIRTUAL_HUB_ALEGRO_DE_EIC.equals(tsoEic);
    }

    private ReportNode checkAlegroGlskSeries(final Network network, final byte[] glskBa, final OffsetDateTime processTargetDate, final ReportNode initialReport) {
        final ReportNode reporterWithAlegro = createReportWithoutAlegroReports(initialReport);
        getAlegroGskSeries(glskBa, processTargetDate.toInstant())
                .forEach(gskSeries -> checkAlegroGSKSeries(
                        gskSeries,
                        network,
                        reporterWithAlegro
                ));
        return reporterWithAlegro;
    }

    void checkAlegroGSKSeries(final GSKSeriesType gskSeries, final Network network, final ReportNode reportNode) {

        final List<AutoNodesType> autoNodes = new ArrayList<>();
        final List<ManualNodesType> manualNodes = new ArrayList<>();
        final List<String> nodeNames = new ArrayList<>();

        gskSeries.getAutoGSKBlock().forEach(autoblock -> autoNodes.addAll(autoblock.getAutoNodes()));
        autoNodes.forEach(autonode -> nodeNames.add(autonode.getNodeName().getV()));

        gskSeries.getManualGSKBlock().forEach(manualblock -> manualNodes.addAll(manualblock.getManualNodes()));
        manualNodes.forEach(manualnode -> nodeNames.add(manualnode.getNodeName().getV()));

        for (String nodeName : nodeNames) {
            List<DanglingLine> danglingLines = network.getDanglingLineStream()
                    .filter(danglingLine -> danglingLine.getPairingKey().equals(nodeName))
                    .toList();
            if (danglingLines.isEmpty()) {
                reportNode.newReportNode()
                        .withMessageTemplate("glsk.node.not.found")
                        .withTypedValue(NODE_ID_KEY, nodeName, "")
                        .withTypedValue(TYPE_KEY, getType(gskSeries), "")
                        .withTypedValue(TSO, gskSeries.getArea().getV(), "")
                        .withUntypedValue("untypedValue", 3.)
                        .withSeverity(TypedValue.WARN_SEVERITY)
                        .add();
            } else if (!danglingLines.getFirst().getTerminal().isConnected()) {
                reportNode.newReportNode()
                        .withMessageTemplate("glsk.node.connected")
                        .withTypedValue(NODE_ID_KEY, nodeName, "")
                        .withTypedValue(TYPE_KEY, getType(gskSeries), "")
                        .withTypedValue(TSO, gskSeries.getArea().getV(), "")
                        .withSeverity(TypedValue.WARN_SEVERITY)
                        .add();
            }
        }
    }

    private String getType(final GSKSeriesType gskSeries) {
        if (gskSeries.getBusinessType().getV().value().equals("Z02")) {
            return GENERATOR;
        } else if (gskSeries.getBusinessType().getV().value().equals("Z05")) {
            return LOAD;
        } else {
            throw new CeMergingException("Error in Glsk Series : unknown ucteBusinessType");
        }
    }

    List<GSKSeriesType> getAlegroGskSeries(final byte[] glskBa, final Instant processTargetDate) {
        final GSKDocument glskDocument = JaxbUtils.readFromBytes(GSKDocument.class, glskBa);
        return glskDocument.getGSKSeries().stream().filter(gskSeries -> containsAlegro(gskSeries, processTargetDate)).toList();
    }

    private boolean containsAlegro(final GSKSeriesType gskSeries, final Instant processTargetDate) {
        return containsAlegroBlock(gskSeries.getAutoGSKBlock(), AutoGSKBlockType::getGSKName, AutoGSKBlockType::getTimeInterval, processTargetDate)
                || containsAlegroBlock(gskSeries.getManualGSKBlock(), ManualGSKBlockType::getGSKName, ManualGSKBlockType::getTimeInterval, processTargetDate);
    }

    private <T> boolean containsAlegroBlock(final List<T> blocks, final Function<T, IdentificationType> gskNameExtractor, final Function<T, TimeIntervalType> timeIntervalExtractor, final Instant processTargetDate) {
        return blocks.stream()
                .anyMatch(block -> isAlegroHubInInterval(
                        gskNameExtractor.apply(block).getV(),
                        timeIntervalExtractor.apply(block).getV(),
                        processTargetDate
                ));
    }

    private boolean isAlegroHubInInterval(final String gskName, final String timeInterval, final Instant processTargetDate) {
        final boolean isAlegroName = VIRTUAL_HUB_ALEGRO_BE_CODE.equals(gskName) || VIRTUAL_HUB_ALEGRO_DE_CODE.equals(gskName);
        if (!isAlegroName) {
            return false;
        }
        final Interval interval = Interval.parse(timeInterval);
        return interval.contains(processTargetDate);
    }

    QualityCheckReport exportQualityReport(final ReportNode reporter, final OffsetDateTime targetDateTime) throws DatatypeConfigurationException {
        final QualityCheckReport qualityCheckReport = new QualityCheckReport();
        fillHeader(qualityCheckReport, targetDateTime);
        fillQualityChecks(reporter, qualityCheckReport, targetDateTime);

        return qualityCheckReport;
    }

    private void fillQualityChecks(final ReportNode reportNode, final QualityCheckReport qualityCheckReport, final OffsetDateTime targetDateTime) {
        final List<QualityCheckType> convertedLogs = reportNode.getChildren().stream()
                .map(report -> toQualityCheckType(report, targetDateTime))
                .toList();

        final QualityChecksType wrapper = new QualityChecksType();
        wrapper.getQualityCheck().addAll(convertedLogs);
        qualityCheckReport.getQualityChecks().add(wrapper);
    }

    private QualityCheckType toQualityCheckType(final ReportNode reportNode, final OffsetDateTime targetDateTime) {
        final QualityCheckType qualityCheckType = new QualityCheckType();
        qualityCheckType.setAssetId(getReportValue(reportNode, NODE_ID_KEY));
        qualityCheckType.setCheckId(reportNode.getMessageKey());
        qualityCheckType.setCheckType(GLSK);
        qualityCheckType.setInfo(getReportValue(reportNode, TYPE_KEY) + " - " + reportNode.getMessage());
        qualityCheckType.setSeverity("WARNING");
        qualityCheckType.setTimeInterval(DateTimeUtils.toHourlyInterval(targetDateTime));
        final AreaType area = new AreaType();
        area.setCodingScheme(CodingSchemeType.A_01);
        area.setV(getReportValue(reportNode, TSO));
        qualityCheckType.setArea(area);
        return qualityCheckType;
    }

    private void fillHeader(QualityCheckReport qualityCheckReport, OffsetDateTime targetDateTime) throws DatatypeConfigurationException {
        qualityCheckReport.setDtdVersion("0");
        qualityCheckReport.setDtdRelease("1");

        IdentificationType identificationObject = new IdentificationType();
        identificationObject.setV(targetDateTime.toString());
        qualityCheckReport.setMessageIdentification(identificationObject);

        VersionType versionType = new VersionType();
        versionType.setV(1);
        qualityCheckReport.setMessageVersion(versionType);

        ProcessType processType = new ProcessType();
        processType.setV(ProcessTypeList.A_48);
        qualityCheckReport.setProcessType(processType);

        PartyType senderPartyType = new PartyType();
        senderPartyType.setCodingScheme(CodingSchemeType.A_01);
        senderPartyType.setV(SENDER_ID);
        qualityCheckReport.setSenderIdentification(senderPartyType);

        RoleType senderRoleType = new RoleType();
        senderRoleType.setV(RoleTypeList.A_44);
        qualityCheckReport.setSenderRole(senderRoleType);

        PartyType receiverPartyType = new PartyType();
        receiverPartyType.setCodingScheme(CodingSchemeType.A_01);
        receiverPartyType.setV(RECEIVER_ID);
        qualityCheckReport.setReceiverIdentification(receiverPartyType);

        RoleType receiverRoleType = new RoleType();
        receiverRoleType.setV(RoleTypeList.A_36);
        qualityCheckReport.setReceiverRole(receiverRoleType);

        MessageDateTimeType messageDateTimeType = new MessageDateTimeType();
        GregorianCalendar c = GregorianCalendar.from(ZonedDateTime.now());
        XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
        xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        xmlGregorianCalendar.setTimezone(0);
        messageDateTimeType.setV(xmlGregorianCalendar);
        qualityCheckReport.setMessageDateTime(messageDateTimeType);

        TimeIntervalType timeIntervalType = new TimeIntervalType();
        timeIntervalType.setV(DateTimeUtils.toHourlyInterval(targetDateTime));
        qualityCheckReport.setQualityCheckTimeInterval(timeIntervalType);

        MessageType messageType = new MessageType();
        messageType.setV(MessageTypeList.A_16);
        qualityCheckReport.setMessageType(messageType);

        AreaType domain = new AreaType();
        domain.setCodingScheme(CodingSchemeType.A_01);
        domain.setV(CORE_REGION_ID);
        qualityCheckReport.setDomain(domain);
    }

    private String getReportValue(ReportNode reportNode, String key) {
        return reportNode.getValue(key)
                .orElseThrow(() -> new IllegalArgumentException("Missing report value: " + key))
                .getValue()
                .toString();
    }
}

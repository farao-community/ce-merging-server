/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.merging.process.FileStorageService;
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
import java.io.InputStream;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.GregorianCalendar;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;

@Service
public class GlskQualityCheckService {
    static final String LOAD = "Load";
    static final String GENERATOR = "Generator";
    static final String NODE_ID_KEY = "NodeId";
    private static final String GLSK = "GLSK";
    static final String TYPE_KEY = "Type";
    static final String TSO_KEY = "TSO";
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskQualityCheckService.class);
    private static final String GLSK_QUALITY_REPORT_URL = "/tasks/%d/artifacts/glsk-quality-report";
    private static final String GLSK_QUALITY_REPORT_FILENAME = "%s_GLSK_QUALITY_CHECK.xml";

    private final CeMergingConfiguration configuration;
    private final GlskFixService glskFixService;
    private final FileStorageService storageService;

    public GlskQualityCheckService(CeMergingConfiguration configuration, GlskFixService glskFixService, FileStorageService storageService) {
        this.configuration = configuration;
        this.glskFixService = glskFixService;
        this.storageService = storageService;
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
            if (Boolean.TRUE.equals(inputs.getMergingWithInternalHvdc())) {
                Network network = Network.read(mergedFile.getPath());
                rootReportNode = checkAlegroGlskSeries(network, glskBytes, processTargetDate, rootReportNode);
            }
            QualityCheckReport xmlReport = exportQualityReport(rootReportNode, processTargetDate);
            saveInArtifacts(xmlReport, task);
            glskFixService.saveInArtifacts(correctedGlsk, task);
        } catch (Exception e) {
            String errorMessage = String.format("GLSK quality check failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
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

    ReportNode removeInitialAlegroReports(ReportNode initialReporter) {
        //delete the log initially generated
        ReportNode rootReportNode = ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME, PowsyblEntsoeReportResourceBundle.BASE_NAME)
                .withMessageTemplate(initialReporter.getMessageKey())
                .build();
        List<ReportNode> reportsWithoutAlegro = initialReporter.getChildren().stream()
                .filter(report -> !isAlegroReport(report))
                .toList();
        reportsWithoutAlegro.forEach(reportNode -> {
            ReportNodeAdder reportNodeAdder = rootReportNode.newReportNode()
                    .withMessageTemplate(reportNode.getMessageKey());

            reportNode.getValues().forEach((key, value) -> reportNodeAdder.withTypedValue(key, value.toString(), ""));

            reportNodeAdder.add();
        });
        return rootReportNode;
    }

    private boolean isAlegroReport(ReportNode reportNode) {
        Optional<TypedValue> typedValue = reportNode.getValue(TSO_KEY);
        if (typedValue.isEmpty()) {
            return false;
        }
        String tsoEic = typedValue.get().toString();
        return VIRTUAL_HUB_ALEGRO_BE_EIC.equals(tsoEic) || VIRTUAL_HUB_ALEGRO_DE_EIC.equals(tsoEic);
    }

    private ReportNode checkAlegroGlskSeries(Network network, byte[] glskBa, OffsetDateTime processTargetDate, ReportNode initialReport) {
        ReportNode reporterWithAlegro = removeInitialAlegroReports(initialReport);
        getAlegroGskSeries(glskBa, processTargetDate.toInstant())
                .forEach(gskSeries -> checkAlegroGSKSeries(
                        gskSeries,
                        network,
                        reporterWithAlegro
                ));
        return reporterWithAlegro;
    }

    void checkAlegroGSKSeries(GSKSeriesType gskSeries, Network network, ReportNode reportNode) {

        List<AutoNodesType> autoNodes = new ArrayList<>();
        List<ManualNodesType> manualNodes = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>();

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
                        .withTypedValue(TSO_KEY, gskSeries.getArea().getV(), "")
                        .withUntypedValue("untypedValue", 3.)
                        .withSeverity(TypedValue.WARN_SEVERITY)
                        .add();
            } else if (!danglingLines.getFirst().getTerminal().isConnected()) {
                reportNode.newReportNode()
                        .withMessageTemplate("glsk.node.connected")
                        .withTypedValue(NODE_ID_KEY, nodeName, "")
                        .withTypedValue(TYPE_KEY, getType(gskSeries), "")
                        .withTypedValue(TSO_KEY, gskSeries.getArea().getV(), "")
                        .withSeverity(TypedValue.WARN_SEVERITY)
                        .add();
            }
        }
    }

    private String getType(GSKSeriesType gskSeries) {
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
        return gskSeries.getAutoGSKBlock().stream()
                .anyMatch(block -> isAlegroBlock(block.getGSKName().getV(), block.getTimeInterval().getV(), processTargetDate))
                || gskSeries.getManualGSKBlock().stream()
                .anyMatch(block -> isAlegroBlock(block.getGSKName().getV(), block.getTimeInterval().getV(), processTargetDate));
    }

    private boolean isAlegroBlock(final String gskName, final String timeInterval, final Instant processTargetDate) {
        final boolean isAlegroName = VIRTUAL_HUB_ALEGRO_BE_CODE.equals(gskName) || VIRTUAL_HUB_ALEGRO_DE_CODE.equals(gskName);
        if (!isAlegroName) {
            return false;
        }
        final Interval interval = Interval.parse(timeInterval);
        return interval.contains(processTargetDate);
    }

    private void saveInArtifacts(final QualityCheckReport report, final MergingTask task) {
        final String fileName = generateGlskQualityCheckFileName(task);
        final SavedFile savedFile = storageService.save(
                configuration.getArtifactsDirectoryPath(task),
                fileName,
                String.format(GLSK_QUALITY_REPORT_URL, task.getId()),
                path -> JaxbUtils.writeToPath(
                        QualityCheckReport.class,
                        report,
                        path)
        );
        task.getArtifacts().putFile(ArtifactType.GLSK_QUALITY_REPORT, savedFile);
    }

    private QualityCheckReport exportQualityReport(ReportNode reporter, OffsetDateTime targetDateTime) throws DatatypeConfigurationException {
        QualityCheckReport qualityCheckReport = new QualityCheckReport();
        fillHeader(qualityCheckReport, targetDateTime);
        fillQualityChecks(reporter, qualityCheckReport, targetDateTime);

        return qualityCheckReport;
    }

    private void fillQualityChecks(ReportNode reportNode, QualityCheckReport qualityCheckReport, OffsetDateTime targetDateTime) {
        final List<QualityCheckType> convertedLogs = reportNode.getChildren().stream()
                .map(report -> toQualityCheckType(report, targetDateTime))
                .toList();

        final QualityChecksType wrapper = new QualityChecksType();
        wrapper.getQualityCheck().addAll(convertedLogs);
        qualityCheckReport.getQualityChecks().add(wrapper);
    }

    private QualityCheckType toQualityCheckType(ReportNode reportNode, OffsetDateTime targetDateTime) {
        final QualityCheckType qualityCheckType = new QualityCheckType();
        qualityCheckType.setAssetId(reportNode.getValue(NODE_ID_KEY).get().toString());
        qualityCheckType.setCheckId(reportNode.getMessageKey());
        qualityCheckType.setCheckType(GLSK);
        qualityCheckType.setInfo(reportNode.getValue(TYPE_KEY).get() + " - " + reportNode.getMessage());
        // all glsk quality report are warnings
        qualityCheckType.setSeverity("WARNING");
        qualityCheckType.setTimeInterval(localDateToInterval(targetDateTime));
        final AreaType area = new AreaType();
        area.setCodingScheme(CodingSchemeType.A_01);
        area.setV(reportNode.getValue(TSO_KEY).get().toString());
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
        timeIntervalType.setV(localDateToInterval(targetDateTime));
        qualityCheckReport.setQualityCheckTimeInterval(timeIntervalType);

        MessageType messageType = new MessageType();
        messageType.setV(MessageTypeList.A_16);
        qualityCheckReport.setMessageType(messageType);

        AreaType domain = new AreaType();
        domain.setCodingScheme(CodingSchemeType.A_01);
        domain.setV(CORE_REGION_ID);
        qualityCheckReport.setDomain(domain);
    }

    private String localDateToInterval(final OffsetDateTime targetDateTime) {
        final Instant startInstant = targetDateTime.withMinute(0).toInstant();
        final Instant endInstant = startInstant.plus(Duration.ofHours(1));
        return String.format("%s/%s", OffsetDateTime.parse(startInstant.toString()), OffsetDateTime.parse(endInstant.toString()));
    }

    private String generateGlskQualityCheckFileName(final MergingTask task) {
        return String.format(GLSK_QUALITY_REPORT_FILENAME, DateTimeUtils.formatTargetDate(task));
    }
}

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
import com.farao_community.farao.ce_merging.common.util.LogsCustomisationUtils;
import com.farao_community.farao.ce_merging.merging.post_process.merging_supervisor_logs.MergingStep;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AreaType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.BusinessTypeList;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.MessageDateTimeType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.MessageType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.PartyType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ProcessType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QualityCheckReport;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QualityCheckType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.QualityChecksType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.RoleType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.TimeIntervalType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.VersionType;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.ReportNodeAdder;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.glsk.ucte.quality_check.GlskQualityProcessor;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Network;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.CORE_REGION_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.EMPTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.RECEIVER_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.REPORT_BASE_NAME;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.SENDER_ID;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_BE_EIC;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.VIRTUAL_HUB_ALEGRO_DE_EIC;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toHourlyInterval;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactFileWithWriter;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isAlegroNode;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.isPairedWith;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.getReportValue;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_CORRECTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GLSK_QUALITY_REPORT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.TGM_FILE_AFTER_RECESSIVITY;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.CodingSchemeType.A_01;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.MessageTypeList.A_16;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.ProcessTypeList.A_48;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.RoleTypeList.A_36;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.RoleTypeList.A_44;
import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;
import static com.powsybl.entsoe.commons.PowsyblEntsoeReportResourceBundle.BASE_NAME;
import static java.util.function.Predicate.not;
import static javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED;

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

    public GlskQualityCheckService(final CeMergingConfiguration configuration,
                                   final GlskFixService glskFixService) {
        this.configuration = configuration;
        this.glskFixService = glskFixService;
    }

    public void runQualityCheck(final MergingTask task) {
        LogsCustomisationUtils.setExtraFieldsInLogsMdc(task, MergingStep.GLSK_QUALITY_CHECK);
        final Inputs inputs = task.getInputs();
        final OffsetDateTime processTargetDate = inputs.getTargetDate();
        final SavedFile mergedFile = task.getArtifacts().getFile(TGM_FILE_AFTER_RECESSIVITY);
        final SavedFile glskFile = inputs.getGenerationLoadShiftKeys();
        try (InputStream mergedFileIs = new FileInputStream(mergedFile.getPath());
             InputStream glskFileIs = new FileInputStream(glskFile.getPath())) {
            final byte[] glskBytes = IOUtils.toByteArray(glskFileIs);
            glskFixService.checkTimeInterval(glskBytes, inputs.getTargetDate());
            ReportNode rootReportNode = createRootReportNode();
            GlskQualityProcessor.process(mergedFile.getOriginalName(), mergedFileIs, new ByteArrayInputStream(glskBytes), processTargetDate.toInstant(), rootReportNode);
            final byte[] correctedGlsk = glskFixService.fixGlsk(glskBytes, rootReportNode, processTargetDate.toInstant());
            saveArtifactFileWithWriter(GLSK_QUALITY_CORRECTED_FILE, task, configuration, writeGlsk(correctedGlsk));

            if (Boolean.TRUE.equals(inputs.getMergingWithInternalHvdc())) {
                Network network = Network.read(mergedFile.getPath());
                rootReportNode = checkAlegroGlskSeries(network, glskBytes, processTargetDate, rootReportNode);
            }

            QualityCheckReport xmlReport = exportQualityReport(rootReportNode, processTargetDate);
            saveArtifactFileWithWriter(GLSK_QUALITY_REPORT, task, configuration, path -> JaxbUtils.writeToPath(QualityCheckReport.class, xmlReport, path));

        } catch (final Exception e) {
            final String errorMessage = String.format("GLSK quality check failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private FileStorageUtils.ThrowingConsumer<Path> writeGlsk(final byte[] glskBytes) {
        return path -> {
            try {
                Files.write(path, glskBytes);
            } catch (IOException e) {
                throw new CeMergingException("Cannot write corrected GLSK artifact file", e);
            }
        };
    }

    private ReportNode createRootReportNode() {
        return ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME, BASE_NAME)
                .withMessageTemplate("glsk.quality.report")
                .build();
    }

    ReportNode createReportWithoutAlegroReports(final ReportNode initialReporter) {
        //delete the log initially generated
        final ReportNode rootReportNode = ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME, BASE_NAME)
                .withMessageTemplate(initialReporter.getMessageKey())
                .build();
        initialReporter.getChildren().stream()
                .filter(not(this::isAlegroReport))
                .forEach(reportNode -> addChildReportNode(rootReportNode, reportNode));
        return rootReportNode;
    }

    private void addChildReportNode(final ReportNode parentNode,
                                    final ReportNode childNode) {
        final ReportNodeAdder reportNodeAdder = parentNode.newReportNode()
                .withMessageTemplate(childNode.getMessageKey());
        childNode.getValues().forEach((key, value) -> reportNodeAdder.withTypedValue(key, value.toString(), EMPTY));
        reportNodeAdder.add();
    }

    private boolean isAlegroReport(final ReportNode reportNode) {
        final Optional<TypedValue> reportNodeValue = reportNode.getValue(TSO);
        if (reportNodeValue.isEmpty()) {
            return false;
        }
        final String tsoEic = reportNodeValue.get().toString();
        return VIRTUAL_HUB_ALEGRO_BE_EIC.equals(tsoEic) || VIRTUAL_HUB_ALEGRO_DE_EIC.equals(tsoEic);
    }

    private ReportNode checkAlegroGlskSeries(final Network network,
                                             final byte[] glskBa,
                                             final OffsetDateTime processTargetDate,
                                             final ReportNode initialReport) {
        final ReportNode reporterWithAlegro = createReportWithoutAlegroReports(initialReport);
        getAlegroGskSeries(glskBa, processTargetDate.toInstant())
                .forEach(gskSeries -> checkAlegroGSKSeries(
                        gskSeries,
                        network,
                        reporterWithAlegro
                ));
        return reporterWithAlegro;
    }

    void checkAlegroGSKSeries(final GSKSeriesType gskSeries,
                              final Network network,
                              final ReportNode reportNode) {

        final Stream<String> autoNodeNames = gskSeries.getAutoGSKBlock()
                .stream()
                .map(AutoGSKBlockType::getAutoNodes)
                .flatMap(List::stream)
                .map(GlskUtils::getName);

        final Stream<String> manualNodeNames = gskSeries.getManualGSKBlock()
                .stream()
                .map(ManualGSKBlockType::getManualNodes)
                .flatMap(List::stream)
                .map(GlskUtils::getName);

        final String type = getType(gskSeries);
        final String area = gskSeries.getArea().getV();

        Stream.concat(autoNodeNames, manualNodeNames)
                .forEach(nodeName -> reportInconsistency(nodeName, network, reportNode, type, area));
    }

    private void reportInconsistency(final String nodeName,
                                     final Network network,
                                     final ReportNode reportNode,
                                     final String type,
                                     final String area) {
        final List<BoundaryLine> boundaryLines = network.getBoundaryLineStream()
                .filter(isPairedWith(nodeName))
                .toList();

        if (boundaryLines.isEmpty()) {
            reportNode.newReportNode()
                    .withMessageTemplate("glsk.node.not.found")
                    .withTypedValue(NODE_ID_KEY, nodeName, EMPTY)
                    .withTypedValue(TYPE_KEY, type, EMPTY)
                    .withTypedValue(TSO, area, EMPTY)
                    .withUntypedValue("untypedValue", 3.)
                    .withSeverity(WARN_SEVERITY)
                    .add();
        } else if (!boundaryLines.getFirst().getTerminal().isConnected()) {
            reportNode.newReportNode()
                    .withMessageTemplate("glsk.node.connected")
                    .withTypedValue(NODE_ID_KEY, nodeName, EMPTY)
                    .withTypedValue(TYPE_KEY, type, EMPTY)
                    .withTypedValue(TSO, area, EMPTY)
                    .withSeverity(WARN_SEVERITY)
                    .add();
        }
    }

    private String getType(final GSKSeriesType gskSeries) {
        return switch (BusinessTypeList.fromValue(gskSeries.getBusinessType().getV().value())) {
            case Z_02 -> GENERATOR;
            case Z_05 -> LOAD;
            default -> throw new CeMergingException("Error in Glsk Series : unknown ucteBusinessType");
        };
    }

    List<GSKSeriesType> getAlegroGskSeries(final byte[] glskBytes,
                                           final Instant processTargetDate) {
        final GSKDocument glskDocument = JaxbUtils.readFromBytes(GSKDocument.class, glskBytes);
        return glskDocument.getGSKSeries().stream().filter(gskSeries -> containsAlegro(gskSeries, processTargetDate)).toList();
    }

    private boolean containsAlegro(final GSKSeriesType gskSeries,
                                   final Instant processTargetDate) {
        return containsAlegroBlock(gskSeries.getAutoGSKBlock(), AutoGSKBlockType::getGSKName, AutoGSKBlockType::getTimeInterval, processTargetDate)
               || containsAlegroBlock(gskSeries.getManualGSKBlock(), ManualGSKBlockType::getGSKName, ManualGSKBlockType::getTimeInterval, processTargetDate);
    }

    private <T> boolean containsAlegroBlock(final List<T> blocks,
                                            final Function<T, IdentificationType> gskNameExtractor,
                                            final Function<T, TimeIntervalType> timeIntervalExtractor,
                                            final Instant processTargetDate) {
        return blocks.stream()
                .anyMatch(block -> isAlegroHubInInterval(
                        gskNameExtractor.apply(block).getV(),
                        timeIntervalExtractor.apply(block).getV(),
                        processTargetDate
                ));
    }

    private boolean isAlegroHubInInterval(final String gskName,
                                          final String timeInterval,
                                          final Instant processTargetDate) {
        if (!isAlegroNode(gskName)) {
            return false;
        }
        final Interval interval = Interval.parse(timeInterval);
        return interval.contains(processTargetDate);
    }

    QualityCheckReport exportQualityReport(final ReportNode reporter,
                                           final OffsetDateTime targetDateTime) throws DatatypeConfigurationException {
        final QualityCheckReport qualityCheckReport = new QualityCheckReport();
        fillHeader(qualityCheckReport, targetDateTime);
        fillQualityChecks(reporter, qualityCheckReport, targetDateTime);

        return qualityCheckReport;
    }

    private void fillQualityChecks(final ReportNode reportNode,
                                   final QualityCheckReport qualityCheckReport,
                                   final OffsetDateTime targetDateTime) {
        final List<QualityCheckType> convertedLogs = reportNode.getChildren().stream()
                .map(report -> toQualityCheckType(report, targetDateTime))
                .toList();

        final QualityChecksType wrapper = new QualityChecksType();
        wrapper.getQualityCheck().addAll(convertedLogs);
        qualityCheckReport.getQualityChecks().add(wrapper);
    }

    private QualityCheckType toQualityCheckType(final ReportNode reportNode,
                                                final OffsetDateTime targetDateTime) {
        final QualityCheckType qualityCheckType = new QualityCheckType();
        qualityCheckType.setAssetId(getReportValue(reportNode, NODE_ID_KEY));
        qualityCheckType.setCheckId(reportNode.getMessageKey());
        qualityCheckType.setCheckType(GLSK);
        qualityCheckType.setInfo(getReportValue(reportNode, TYPE_KEY) + " - " + reportNode.getMessage());
        qualityCheckType.setSeverity("WARNING");
        qualityCheckType.setTimeInterval(toHourlyInterval(targetDateTime));
        final AreaType area = new AreaType();
        area.setCodingScheme(A_01);
        area.setV(getReportValue(reportNode, TSO));
        qualityCheckType.setArea(area);

        return qualityCheckType;
    }

    private void fillHeader(final QualityCheckReport qualityCheckReport,
                            final OffsetDateTime targetDateTime) throws DatatypeConfigurationException {
        qualityCheckReport.setDtdVersion("0");
        qualityCheckReport.setDtdRelease("1");

        final IdentificationType identificationObject = new IdentificationType();
        identificationObject.setV(targetDateTime.toString());
        qualityCheckReport.setMessageIdentification(identificationObject);

        VersionType versionType = new VersionType();
        versionType.setV(1);
        qualityCheckReport.setMessageVersion(versionType);

        final ProcessType processType = new ProcessType();
        processType.setV(A_48);
        qualityCheckReport.setProcessType(processType);

        final PartyType senderPartyType = new PartyType();
        senderPartyType.setCodingScheme(A_01);
        senderPartyType.setV(SENDER_ID);
        qualityCheckReport.setSenderIdentification(senderPartyType);

        final RoleType senderRoleType = new RoleType();
        senderRoleType.setV(A_44);
        qualityCheckReport.setSenderRole(senderRoleType);

        final PartyType receiverPartyType = new PartyType();
        receiverPartyType.setCodingScheme(A_01);
        receiverPartyType.setV(RECEIVER_ID);
        qualityCheckReport.setReceiverIdentification(receiverPartyType);

        final RoleType receiverRoleType = new RoleType();
        receiverRoleType.setV(A_36);
        qualityCheckReport.setReceiverRole(receiverRoleType);

        final MessageDateTimeType messageDateTimeType = new MessageDateTimeType();
        final GregorianCalendar calendar = GregorianCalendar.from(ZonedDateTime.now());
        final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        xmlGregorianCalendar.setMillisecond(FIELD_UNDEFINED);
        xmlGregorianCalendar.setTimezone(0);
        messageDateTimeType.setV(xmlGregorianCalendar);
        qualityCheckReport.setMessageDateTime(messageDateTimeType);

        final TimeIntervalType timeIntervalType = new TimeIntervalType();
        timeIntervalType.setV(toHourlyInterval(targetDateTime));
        qualityCheckReport.setQualityCheckTimeInterval(timeIntervalType);
        final MessageType messageType = new MessageType();
        messageType.setV(A_16);
        qualityCheckReport.setMessageType(messageType);

        final AreaType domain = new AreaType();
        domain.setCodingScheme(A_01);
        domain.setV(CORE_REGION_ID);
        qualityCheckReport.setDomain(domain);
    }
}

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
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.powsybl.commons.report.ReportNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.nio.file.Files;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.PARIS_ZONE_ID;

@Service
public class GlskFixService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskFixService.class);
    public static final String ACTUAL_GLSK_REPORT_CORRECTION_URL = "/tasks/%d/artifacts/actual-glsk-report-correction";
    public static final String GLSK_CORRECTED_NAME = "glsk_corrected_%s_2D%s_UX0.xml";

    private final CeMergingConfiguration configuration;
    private final FileStorageService storage;

    public GlskFixService(CeMergingConfiguration configuration, FileStorageService service) {
        this.configuration = configuration;
        this.storage = service;
    }

    public byte[] fixGlsk(byte[] glskBytes, ReportNode reportNode, Instant targetDate) {
        return fixGlsk(glskBytes, reportNode, targetDate, getNowDate());
    }

    public byte[] fixGlsk(byte[] glskBytes, final ReportNode reportNode, final Instant targetDate, final XMLGregorianCalendar fileCreationDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        updateCreationDate(glskDocument, fileCreationDate);
        fixSeries(reportNode, targetDate, glskDocument);
        return exportGLsk(glskDocument);
    }

    private void fixSeries(final ReportNode reportNode, final Instant targetDate, final GSKDocument glskDocument) {
        final List<GSKSeriesType> removeGskSeriesTypes = new ArrayList<>();
        final GlskFixContext context = new GlskFixContext();
        glskDocument.getGSKSeries().forEach(glskSeries -> {
            GlskBlockFix.validateAndRemoveInvalidGskBlocks(
                    context.getIncorrectBlock(),
                    context.getCorrectBlock(),
                    glskSeries,
                    targetDate,
                    getQualityLogs(reportNode, glskSeries.getArea().getV())
            );
            if (isEmptyGlskSeries(glskSeries)) {
                removeGskSeriesTypes.add(glskSeries);
                storeSerieValue(context, glskSeries, false);
            } else {
                storeSerieValue(context, glskSeries, true);
            }
        });

        glskDocument.getGSKSeries().removeAll(removeGskSeriesTypes);
        GlskSerieRedispatcher.redispatchShareValue(context.getCorrectSerie(), glskDocument);
    }

    private void storeSerieValue(final GlskFixContext context, final GSKSeriesType glskSeries, final boolean correct) {
        final String timeSeriesIdentification = glskSeries.getTimeSeriesIdentification().getV();
        final String area = glskSeries.getArea().getV();
        final double shareValue = glskSeries.getBusinessType().getShare().doubleValue();
        GlskSerieRedispatcher.storeValue(
                correct ? context.getCorrectSerie() : context.getIncorrectSerie(),
                area,
                timeSeriesIdentification,
                shareValue);
    }

    private void updateCreationDate(final GSKDocument glskDocument, final XMLGregorianCalendar fileCreationDate) {
        glskDocument.getCreationDateTime().setV(fileCreationDate);
    }

    private static boolean isEmptyGlskSeries(GSKSeriesType glskSeries) {
        return glskSeries.getAutoGSKBlock().isEmpty() &&
                glskSeries.getManualGSKBlock().isEmpty() &&
                (glskSeries.getCountryGSKBlock() == null || glskSeries.getCountryGSKBlock().isEmpty());
    }

    private List<ReportNode> getQualityLogs(final ReportNode reportNode, final String tso) {
        return reportNode.getChildren().stream()
                .filter(report -> report.getValue("TSO")
                        .map(Object::toString)
                        .filter(tso::equals)
                        .isPresent())
                .toList();
    }

    private GSKDocument importGlskDocument(byte[] glskBytes) {
        return JaxbUtils.readFromBytes(GSKDocument.class, glskBytes);
    }

    private byte[] exportGLsk(GSKDocument glskDocument) {
        return JaxbUtils.writeToBytes(GSKDocument.class, glskDocument);
    }

    private XMLGregorianCalendar getNowDate() {
        try {
            final GregorianCalendar calendar = GregorianCalendar.from(ZonedDateTime.now(PARIS_ZONE_ID));
            final XMLGregorianCalendar xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            xmlGregorianCalendar.setTimezone(0);
            return xmlGregorianCalendar;
        } catch (DatatypeConfigurationException e) {
            throw new CeMergingException("Cannot create XMLGregorianCalendar date for fixed glsk document, " + e.getMessage());
        }
    }

    protected void saveInArtifacts(byte[] file, MergingTask task) {
        final String fileName = generateGlskFileName(task);
        final SavedFile savedFile = storage.save(configuration.getArtifactsDirectoryPath(task),
                fileName,
                String.format(ACTUAL_GLSK_REPORT_CORRECTION_URL, task.getId()),
                path -> Files.write(path, file));
        task.getArtifacts().putFile(ArtifactType.GLSK_QUALITY_CORRECTED_FILE, savedFile);

    }

    private String generateGlskFileName(MergingTask task) {
        return String.format(GLSK_CORRECTED_NAME, DateTimeUtils.formatTargetDate(task), DateTimeUtils.dayOfWeek(task));
    }

    void checkTimeInterval(byte[] glskBytes, OffsetDateTime targetDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        final Interval timeInterval = Interval.parse(glskDocument.getGSKTimeInterval().getV());
        if (!timeInterval.contains(targetDate.toInstant())) {
            LOGGER.error("The time interval of Glsk document does not correspond to the target process date {} ", targetDate);
            throw new CeMergingException("The time interval of Glsk document does not correspond to the target process date " + targetDate);
        }
    }

    private static class GlskFixContext {

        private final Map<String, List<GlskRedispatchingEntity>> incorrectBlock = new HashMap<>();
        private final Map<String, List<GlskRedispatchingEntity>> correctBlock = new HashMap<>();
        private final Map<String, List<GlskRedispatchingEntity>> incorrectSerie = new HashMap<>();
        private final Map<String, List<GlskRedispatchingEntity>> correctSerie = new HashMap<>();

        public Map<String, List<GlskRedispatchingEntity>> getIncorrectBlock() {
            return incorrectBlock;
        }

        public Map<String, List<GlskRedispatchingEntity>> getCorrectBlock() {
            return correctBlock;
        }

        public Map<String, List<GlskRedispatchingEntity>> getIncorrectSerie() {
            return incorrectSerie;
        }

        public Map<String, List<GlskRedispatchingEntity>> getCorrectSerie() {
            return correctSerie;
        }
    }
}

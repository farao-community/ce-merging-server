/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.powsybl.commons.report.ReportNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.threeten.extra.Interval;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;

@Service
public class GlskFixService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskFixService.class);

    public byte[] fixGlsk(final byte[] glskBytes, final ReportNode reportNode, final Instant targetDate) {
        return fixGlsk(glskBytes, reportNode, targetDate, DateTimeUtils.getNowDate());
    }

    public byte[] fixGlsk(final byte[] glskBytes, final ReportNode reportNode, final Instant targetDate, final XMLGregorianCalendar fileCreationDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        updateCreationDate(glskDocument, fileCreationDate);
        fixGlskSeries(reportNode, targetDate, glskDocument);
        return exportGLsk(glskDocument);
    }

    private void fixGlskSeries(final ReportNode reportNode, final Instant targetDate, final GSKDocument glskDocument) {
        final List<GSKSeriesType> invalidGskSeries = new ArrayList<>();
        final GlskFixContext context = new GlskFixContext();
        glskDocument.getGSKSeries().forEach(glskSeries -> {
            GlskBlockFix.removeInvalidGskBlocks(
                    context.getIncorrectBlockByGskName(),
                    context.getCorrectBlockByGskName(),
                    glskSeries,
                    targetDate,
                    getQualityLogs(reportNode, glskSeries.getArea().getV())
            );
            if (isEmpty(glskSeries)) {
                invalidGskSeries.add(glskSeries);
            } else {
                storeSerieValue(context.getCorrectSeriesByArea(), glskSeries);
            }
        });

        glskDocument.getGSKSeries().removeAll(invalidGskSeries);
        GlskSerieRedispatcher.redispatchShareValue(context.getCorrectSeriesByArea(), glskDocument);
    }

    private void storeSerieValue(final Map<String, List<GlskRedispatchingEntity>> correctSeries, final GSKSeriesType glskSeries) {
        final String timeSeriesIdentification = glskSeries.getTimeSeriesIdentification().getV();
        final String area = glskSeries.getArea().getV();
        final double shareValue = glskSeries.getBusinessType().getShare().doubleValue();
        GlskSerieRedispatcher.storeValue(correctSeries, area, timeSeriesIdentification, shareValue);
    }

    private void updateCreationDate(final GSKDocument glskDocument, final XMLGregorianCalendar fileCreationDate) {
        glskDocument.getCreationDateTime().setV(fileCreationDate);
    }

    private static boolean isEmpty(final GSKSeriesType glskSeries) {
        return glskSeries.getAutoGSKBlock().isEmpty() &&
                glskSeries.getManualGSKBlock().isEmpty() &&
                CollectionUtils.isEmpty(glskSeries.getCountryGSKBlock());
    }

    private List<ReportNode> getQualityLogs(final ReportNode reportNode, final String tso) {
        return reportNode.getChildren().stream()
                .filter(report -> report.getValue(TSO)
                        .map(Object::toString)
                        .filter(tso::equals)
                        .isPresent())
                .toList();
    }

    private GSKDocument importGlskDocument(final byte[] glskBytes) {
        return JaxbUtils.readFromBytes(GSKDocument.class, glskBytes);
    }

    private byte[] exportGLsk(final GSKDocument glskDocument) {
        return JaxbUtils.writeToBytes(GSKDocument.class, glskDocument);
    }

    void checkTimeInterval(final byte[] glskBytes, final OffsetDateTime targetDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        final Interval timeInterval = Interval.parse(glskDocument.getGSKTimeInterval().getV());
        if (!timeInterval.contains(targetDate.toInstant())) {
            LOGGER.error("The time interval of Glsk document does not correspond to the target process date {} ", targetDate);
            throw new CeMergingException("The time interval of Glsk document does not correspond to the target process date " + targetDate);
        }
    }

    private static class GlskFixContext {

        private final Map<String, List<GlskRedispatchingEntity>> incorrectBlockByGskName = new HashMap<>();
        private final Map<String, List<GlskRedispatchingEntity>> correctBlockByGskName = new HashMap<>();
        private final Map<String, List<GlskRedispatchingEntity>> correctSeriesByArea = new HashMap<>();

        public Map<String, List<GlskRedispatchingEntity>> getIncorrectBlockByGskName() {
            return incorrectBlockByGskName;
        }

        public Map<String, List<GlskRedispatchingEntity>> getCorrectBlockByGskName() {
            return correctBlockByGskName;
        }

        public Map<String, List<GlskRedispatchingEntity>> getCorrectSeriesByArea() {
            return correctSeriesByArea;
        }
    }
}

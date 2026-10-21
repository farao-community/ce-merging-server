/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskFixContext;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.powsybl.commons.report.ReportNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.TSO;
import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.getNowDate;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.addShare;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskUtils.hasEntry;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
public class GlskFixService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskFixService.class);

    public byte[] fixGlsk(final byte[] glskBytes,
                          final ReportNode reportNode,
                          final Instant targetDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        glskDocument.getCreationDateTime().setV(getNowDate());
        final GlskFixContext context = new GlskFixContext();
        final List<GSKSeriesType> glskSeries = glskDocument.getGSKSeries();

        final List<GSKSeriesType> invalidSeries = glskSeries.stream()
                .map(gsk -> fixSeries(gsk, targetDate, context, reportNode))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        glskSeries.removeAll(invalidSeries);

        GlskSerieRedispatcher.redispatchShare(context.getCorrectSeriesByArea(), glskDocument);

        return JaxbUtils.writeToBytes(GSKDocument.class, glskDocument);
    }

    private Optional<GSKSeriesType> fixSeries(final GSKSeriesType glskSeries,
                                              final Instant targetDate,
                                              final GlskFixContext context,
                                              final ReportNode reportNode) {
        final String area = glskSeries.getArea().getV();

        GlskBlockFix.removeInvalidGskBlocks(
                context.getIncorrectBlockByGskName(),
                context.getCorrectBlockByGskName(),
                glskSeries,
                targetDate,
                getQualityLogs(reportNode, area)
        );
        if (hasNoBlocks(glskSeries)) {
            return Optional.of(glskSeries);
        } else {
            final String timeSeriesIdentification = glskSeries.getTimeSeriesIdentification().getV();
            final double shareValue = glskSeries.getBusinessType().getShare().doubleValue();
            addShare(context.getCorrectSeriesByArea(), area, timeSeriesIdentification, shareValue);
            return Optional.empty();
        }
    }

    private static boolean hasNoBlocks(final GSKSeriesType glskSeries) {
        return isEmpty(glskSeries.getAutoGSKBlock()) &&
               isEmpty(glskSeries.getManualGSKBlock()) &&
               isEmpty(glskSeries.getCountryGSKBlock());
    }

    private List<ReportNode> getQualityLogs(final ReportNode reportNode,
                                            final String tso) {
        return reportNode.getChildren()
                .stream()
                .filter(hasEntry(TSO, tso))
                .toList();
    }

    private GSKDocument importGlskDocument(final byte[] glskBytes) {
        return JaxbUtils.readFromBytes(GSKDocument.class, glskBytes);
    }

    void checkTimeInterval(final byte[] glskBytes,
                           final OffsetDateTime targetDate) {
        final GSKDocument glskDocument = importGlskDocument(glskBytes);
        final Interval timeInterval = Interval.parse(glskDocument.getGSKTimeInterval().getV());
        if (!timeInterval.contains(targetDate.toInstant())) {
            LOGGER.error("The time interval of Glsk document does not correspond to the target process date {} ", targetDate);
            throw new CeMergingException("The time interval of Glsk document does not correspond to the target process date " + targetDate);
        }
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.process.glsk_fix.model.GlskSerieShare;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumProperty;

public final class GlskSerieRedispatcher {
    private static final double TARGET_SHARE_VALUE = 100.0;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskSerieRedispatcher.class);

    private GlskSerieRedispatcher() {
        // Utility class
    }

    static void redispatchShare(final Map<String, List<GlskSerieShare>> sharesByArea,
                                final GSKDocument gskDocument) {
        sharesByArea.forEach((area, serieShares) -> {
            final double shareSum = sumProperty(serieShares, GlskSerieShare::share);
            if (shareSum < TARGET_SHARE_VALUE) {
                LOGGER.info("The sum of shares for area {} is {}, a share adjustment will be made.", area, shareSum);
                final double shareAdjustment = TARGET_SHARE_VALUE - shareSum;
                serieShares.forEach(serieShare -> updateGskShare(serieShare, shareAdjustment, shareSum, gskDocument));
            }
        });
    }

    private static void updateGskShare(final GlskSerieShare serieShare,
                                       final double adjustment,
                                       final double totalShare,
                                       final GSKDocument gskDocument) {
        if (totalShare == 0.0) {
            throw new CeMergingException("Division by zero: share sum cannot be zero");
        }
        final double initialShare = serieShare.share();
        final BigDecimal updatedShare = BigDecimal.valueOf(Math.round(initialShare + (initialShare / totalShare) * adjustment));

        getGSKSeriesById(gskDocument, serieShare.id())
                .ifPresent(gskSeriesType -> {
                    LOGGER.info("Updating the share of GSKSeries with id {} from {} to {}.", serieShare.id(), initialShare, updatedShare);
                    gskSeriesType.getBusinessType().setShare(updatedShare);
                });
    }

    private static Optional<GSKSeriesType> getGSKSeriesById(final GSKDocument gskDocument,
                                                            final String timeSerieId) {
        return gskDocument.getGSKSeries().stream()
                .filter(hasId(timeSerieId))
                .findFirst();
    }

    private static Predicate<GSKSeriesType> hasId(final String id) {
        return gskSeries -> gskSeries.getTimeSeriesIdentification().getV().equals(id);
    }

}

package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GlskSerieRedispatcher {
    private static final double TARGET_SHARE_VALUE = 100.0;
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskSerieRedispatcher.class);

    private GlskSerieRedispatcher() {
        // Utility class
    }

    static void storeValue(final Map<String, List<GlskRedispatchingEntity>> map, final String area, final String timeSeriesIdentification, final double shareValue) {
        map.computeIfAbsent(area, s -> new ArrayList<>()).add(new GlskRedispatchingEntity(timeSeriesIdentification, shareValue));
    }

    static void redispatchShareValue(final Map<String, List<GlskRedispatchingEntity>> valuesByArea, final GSKDocument gskDocument) {
        valuesByArea.forEach((area, entities) -> {
            final double shareSum = calculateShareSum(entities);
            if (shareSum < TARGET_SHARE_VALUE) {
                LOGGER.info("The sum of shares for area {} is {}, a share adjustment will be made.", area, shareSum);
                final double shareAdjustment = TARGET_SHARE_VALUE - shareSum;
                entities.forEach(entity ->
                    updateGskShare(entity, shareAdjustment, shareSum, gskDocument)
                );
            }
        });
    }

    private static void updateGskShare(final GlskRedispatchingEntity entity, final double adjustment, final double shareSum, final GSKDocument gskDocument) {
        if (Double.compare(shareSum, 0.0) == 0) {
            throw new CeMergingException("Division by zero: share sum cannot be zero");
        }
        final double initialShare = entity.getShare();
        final long updatedShare = Math.round(initialShare + (initialShare / shareSum) * adjustment);
        getGSKSeriesTypeById(gskDocument, entity.getId())
                .ifPresent(gskSeriesType -> {
                    LOGGER.info("Updating the share of GSKSeries with id {} from {} to {}.", entity.getId(), initialShare, updatedShare);
                    gskSeriesType.getBusinessType().setShare(BigDecimal.valueOf(updatedShare));
                });
    }

    private static Optional<GSKSeriesType> getGSKSeriesTypeById(final GSKDocument gskDocument, final String id) {
        return gskDocument.getGSKSeries().stream()
                .filter(gskSeriesType -> gskSeriesType.getTimeSeriesIdentification().getV().equals(id))
                .findFirst();
    }

    private static double calculateShareSum(final List<GlskRedispatchingEntity> entities) {
        return entities.stream()
                .mapToDouble(GlskRedispatchingEntity::getShare)
                .sum();
    }

}

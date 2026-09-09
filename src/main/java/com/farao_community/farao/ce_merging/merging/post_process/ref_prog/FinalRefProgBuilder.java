/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.post_process.ref_prog;

import com.farao_community.farao.ce_merging.common.CeMergingConstants;
import com.farao_community.farao.ce_merging.common.util.DateTimeUtils;
import com.farao_community.farao.ce_merging.common.util.OutputUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.Border;
import com.farao_community.farao.ce_merging.merging.post_process.common.CurrentType;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.ref_prog.PublicationDocument;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.farao_community.farao.ce_merging.common.util.CountryCodeUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.merging.post_process.ref_prog.FinalRefProgHelper.getDocumentIdentification;
import static com.farao_community.farao.ce_merging.merging.post_process.ref_prog.FinalRefProgHelper.getDocumentVersion;
import static com.farao_community.farao.ce_merging.merging.post_process.ref_prog.FinalRefProgHelper.getPublicationTimeInterval;
import static com.farao_community.farao.ce_merging.merging.post_process.ref_prog.FinalRefProgHelper.getReceiverIdentification;
import static com.farao_community.farao.ce_merging.merging.post_process.ref_prog.FinalRefProgHelper.getSenderIdentification;

public final class FinalRefProgBuilder {

    private static final String MAW = "MAW";
    private static final String BUSINESS_TYPE = "A66";

    private FinalRefProgBuilder() {
    }

    private static final String DOCUMENT_TYPE = "A45";
    private static final String CODING_SCHEME = "A01";
    private static final String SENDER_ROLE = "A44";
    private static final String RECEIVER_ROLE = "A36";

    public static PublicationDocument buildFinalRefProgResult(final RefProgResult refProgResult, final MergingTask taskEntity) {
        final String dailyTimeInterval = refProgResult.dailyTimeInterval();
        final OffsetDateTime periodStart = OffsetDateTime.parse(dailyTimeInterval.substring(0, 17), DateTimeFormatter.ISO_DATE_TIME);
        final OffsetDateTime periodEnd = OffsetDateTime.parse(dailyTimeInterval.substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
        final int position = OutputUtils.calculateTargetPosition(taskEntity.getInputs().getTargetDate(), periodStart, periodEnd);
        final String documentIdentification = OutputUtils.getDocumentIdentificationDate(dailyTimeInterval);

        final PublicationDocument publicationDocument = new PublicationDocument();
        publicationDocument.setDtdRelease(BigInteger.valueOf(0));
        publicationDocument.setDtdVersion(BigInteger.valueOf(1));

        publicationDocument.setDocumentIdentification(getDocumentIdentification(documentIdentification));
        publicationDocument.setDocumentVersion(getDocumentVersion());
        final PublicationDocument.DocumentType documentType = new PublicationDocument.DocumentType();
        documentType.setV(DOCUMENT_TYPE);
        publicationDocument.setDocumentType(documentType);

        publicationDocument.setSenderIdentification(getSenderIdentification());

        final PublicationDocument.SenderRole senderRole = new PublicationDocument.SenderRole();
        senderRole.setV(SENDER_ROLE);
        publicationDocument.setSenderRole(senderRole);

        publicationDocument.setReceiverIdentification(getReceiverIdentification());

        final PublicationDocument.ReceiverRole receiverRole = new PublicationDocument.ReceiverRole();
        receiverRole.setV(RECEIVER_ROLE);
        publicationDocument.setReceiverRole(receiverRole);

        final PublicationDocument.CreationDateTime creationDateTime = new PublicationDocument.CreationDateTime();
        creationDateTime.setV(DateTimeUtils.getNowDate());
        publicationDocument.setCreationDateTime(creationDateTime);

        publicationDocument.setPublicationTimeInterval(getPublicationTimeInterval(dailyTimeInterval));

        final PublicationDocument.Domain domain = new PublicationDocument.Domain();
        domain.setCodingScheme(CODING_SCHEME);
        domain.setV(CeMergingConstants.CORE_REGION_ID);
        publicationDocument.setDomain(domain);

        final List<PublicationDocument.PublicationTimeSeries> publicationTimeSeries = computeAllTimeSeries(refProgResult,
                                                                                                     taskEntity,
                                                                                                     dailyTimeInterval,
                                                                                                     position);
        publicationDocument.getPublicationTimeSeries().addAll(publicationTimeSeries);
        return publicationDocument;
    }

    private static List<PublicationDocument.PublicationTimeSeries> computeAllTimeSeries(final RefProgResult refProgResult,
                                                                                 final MergingTask mergingTask,
                                                                                 final String dailyTimeInterval,
                                                                                 final int position) {
        final List<PublicationDocument.PublicationTimeSeries> pubTimeSeriesList = new ArrayList<>();
        final BiMap<String, String> allAreasBiMap = HashBiMap.create(mergingTask.getConfigurations().getRegionConfiguration().getAreasAll());
        final List<VirtualHubRecord> virtualHubList = mergingTask.getConfigurations().getVirtualHubList();
        final Map<String, String> countryMaEicCodeMap = virtualHubList.stream().collect(Collectors.toUnmodifiableMap(VirtualHubRecord::getRelatedMaEic, VirtualHubRecord::getRelatedMaCode, (a, b) -> a));
        final Map<String, String> countryEicCodeMap = virtualHubList.stream().collect(Collectors.toUnmodifiableMap(VirtualHubRecord::getEic, VirtualHubRecord::getCode, (a, b) -> a));

        refProgResult.acExchanges().entrySet().forEach(acExchange -> pubTimeSeriesList.add(computePublicationTimeSeries(acExchange, allAreasBiMap, countryMaEicCodeMap, countryEicCodeMap, CurrentType.AC, dailyTimeInterval, position)));
        refProgResult.virtualHubsExchanges().entrySet().forEach(virtualHub -> pubTimeSeriesList.add(computePublicationTimeSeries(virtualHub, allAreasBiMap, countryMaEicCodeMap, countryEicCodeMap, CurrentType.DC, dailyTimeInterval, position)));
        return pubTimeSeriesList;
    }

    private static PublicationDocument.PublicationTimeSeries computePublicationTimeSeries(final Map.Entry<Border, Double> entry,
                                                                                   final BiMap<String, String> allAreasBiMap,
                                                                                   final Map<String, String> countryMaEicCodeMap,
                                                                                   final Map<String, String> countryEicCodeMap,
                                                                                   final CurrentType currentType,
                                                                                   final String dailyTimeInterval,
                                                                                   final int position) {
        final PublicationDocument.PublicationTimeSeries publTimeSeries = new PublicationDocument.PublicationTimeSeries();
        final List<PublicationDocument.PublicationTimeSeries.Period.Interval> intervalList = new ArrayList<>();
        final PublicationDocument.PublicationTimeSeries.Period.Interval interval = new PublicationDocument.PublicationTimeSeries.Period.Interval();
        final PublicationDocument.PublicationTimeSeries.Period.Interval.Pos pos = new PublicationDocument.PublicationTimeSeries.Period.Interval.Pos();
        pos.setV(BigInteger.valueOf(position));
        final PublicationDocument.PublicationTimeSeries.Period.Interval.Qty qty = new PublicationDocument.PublicationTimeSeries.Period.Interval.Qty();
        final BigDecimal bigDecimal = BigDecimal.valueOf(entry.getValue()).setScale(0, RoundingMode.HALF_UP);
        qty.setV(bigDecimal.toBigInteger());
        interval.setPos(pos);
        interval.setQty(qty);
        intervalList.add(interval);

        final PublicationDocument.PublicationTimeSeries.Period period = new PublicationDocument.PublicationTimeSeries.Period();
        period.getInterval().addAll(intervalList);
        final PublicationDocument.PublicationTimeSeries.Period.Resolution resol = new PublicationDocument.PublicationTimeSeries.Period.Resolution();
        resol.setV(CeMergingConstants.RESOLUTION);
        period.setResolution(resol);
        final PublicationDocument.PublicationTimeSeries.Period.TimeInterval timeInter = new PublicationDocument.PublicationTimeSeries.Period.TimeInterval();
        timeInter.setV(dailyTimeInterval);
        period.setTimeInterval(timeInter);
        publTimeSeries.setPeriod(period);

        final PublicationDocument.PublicationTimeSeries.TimeSeriesIdentification timeSeriesIdentif = new PublicationDocument.PublicationTimeSeries.TimeSeriesIdentification();
        if (currentType == CurrentType.AC) {

            timeSeriesIdentif.setV(getAcTimeSeriesIdentification(entry, allAreasBiMap));
            publTimeSeries.setTimeSeriesIdentification(timeSeriesIdentif);
        } else {
            timeSeriesIdentif.setV(getDcTimeSeriesIdentification(entry, countryMaEicCodeMap, countryEicCodeMap));
            publTimeSeries.setTimeSeriesIdentification(timeSeriesIdentif);
        }
        final PublicationDocument.PublicationTimeSeries.BusinessType businessType = new PublicationDocument.PublicationTimeSeries.BusinessType();
        businessType.setV(BUSINESS_TYPE);
        publTimeSeries.setBusinessType(businessType);

        final PublicationDocument.PublicationTimeSeries.MeasureUnitQuantity measureUnitQuant = new PublicationDocument.PublicationTimeSeries.MeasureUnitQuantity();
        measureUnitQuant.setV(MAW);
        publTimeSeries.setMeasureUnitQuantity(measureUnitQuant);

        final PublicationDocument.PublicationTimeSeries.InArea inArea = new PublicationDocument.PublicationTimeSeries.InArea();
        inArea.setV(entry.getKey().getInArea());
        inArea.setCodingScheme(CODING_SCHEME);
        publTimeSeries.setInArea(inArea);

        final PublicationDocument.PublicationTimeSeries.OutArea outArea = new PublicationDocument.PublicationTimeSeries.OutArea();
        outArea.setV(entry.getKey().getOutArea());
        outArea.setCodingScheme(CODING_SCHEME);
        publTimeSeries.setOutArea(outArea);
        return publTimeSeries;
    }

    private static String getAcTimeSeriesIdentification(final Map.Entry<Border, Double> entry, final BiMap<String, String> allAreasBiMap) {
        final String countryTo = CountryCodeUtils.mapXkToKs(allAreasBiMap.inverse().get(entry.getKey().getInArea()));
        final String countryFrom = CountryCodeUtils.mapXkToKs(allAreasBiMap.inverse().get(entry.getKey().getOutArea()));
        return countryFrom + "-" + countryTo;
    }

    private static String getDcTimeSeriesIdentification(final Map.Entry<Border, Double> entry,
                                                 final Map<String, String> countryMaEicCodeMap,
                                                 final Map<String, String> countryEicCodeMap) {

        final String eicCodeFrom = entry.getKey().getOutArea();
        final String eicCodeTo = entry.getKey().getInArea();

        final String countryEicCodeFrom = countryMaEicCodeMap.get(eicCodeFrom);
        if (countryEicCodeFrom == null) {
            throw new CeMergingException("Cannot find a virtual hub for EIC: " + eicCodeFrom);
        }

        final String virtualHubEicCodeTo = countryEicCodeMap.get(eicCodeTo);
        if (virtualHubEicCodeTo == null) {
            throw new CeMergingException("Cannot find a virtual hub in direction to EIC: " + eicCodeTo);
        }

        return countryEicCodeFrom + "-" + virtualHubEicCodeTo;
    }
}

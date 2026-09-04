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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 * @author Oualid Aloui {@literal <oualid.aloui at rte-france.com>}
 */
@Service
public class FinalRefProgBuilder {

    public PublicationDocument buildFinalRefProgResult(final RefProgResult refProgResult, final MergingTask taskEntity) {
        final String dailyTimeInterval = refProgResult.dailyTimeInterval();
        final OffsetDateTime periodStart = OffsetDateTime.parse(dailyTimeInterval.substring(0, 17), DateTimeFormatter.ISO_DATE_TIME);
        final OffsetDateTime periodEnd = OffsetDateTime.parse(dailyTimeInterval.substring(18, 35), DateTimeFormatter.ISO_DATE_TIME);
        final int position = OutputUtils.calculateTargetPosition(taskEntity.getInputs().getTargetDate(), periodStart, periodEnd);
        final String documentIdentification = OutputUtils.getDocumentIdentificationDate(dailyTimeInterval);

        final PublicationDocument publicationDocument = new PublicationDocument();
        publicationDocument.setDtdRelease(BigInteger.valueOf(0));
        publicationDocument.setDtdVersion(BigInteger.valueOf(1));

        final PublicationDocument.DocumentIdentification documentIdent = new PublicationDocument.DocumentIdentification();
        documentIdent.setV(documentIdentification);
        publicationDocument.setDocumentIdentification(documentIdent);
        final PublicationDocument.DocumentVersion documentVersion = new PublicationDocument.DocumentVersion();
        documentVersion.setV(BigInteger.valueOf(5));
        publicationDocument.setDocumentVersion(documentVersion);
        final PublicationDocument.DocumentType documentType = new PublicationDocument.DocumentType();
        documentType.setV("A45");
        publicationDocument.setDocumentType(documentType);

        final PublicationDocument.SenderIdentification senderIdentification = new PublicationDocument.SenderIdentification();
        senderIdentification.setCodingScheme("A01");
        senderIdentification.setV(CeMergingConstants.SENDER_ID);
        publicationDocument.setSenderIdentification(senderIdentification);

        final PublicationDocument.SenderRole senderRole = new PublicationDocument.SenderRole();
        senderRole.setV("A44");
        publicationDocument.setSenderRole(senderRole);

        final PublicationDocument.ReceiverIdentification receiverIdentification = new PublicationDocument.ReceiverIdentification();
        receiverIdentification.setCodingScheme("A01");
        receiverIdentification.setV(CeMergingConstants.RECEIVER_ID);
        publicationDocument.setReceiverIdentification(receiverIdentification);

        final PublicationDocument.ReceiverRole receiverRole = new PublicationDocument.ReceiverRole();
        receiverRole.setV("A36");
        publicationDocument.setReceiverRole(receiverRole);

        final PublicationDocument.CreationDateTime creationDateTime = new PublicationDocument.CreationDateTime();
        creationDateTime.setV(DateTimeUtils.getNowDate());
        publicationDocument.setCreationDateTime(creationDateTime);

        final PublicationDocument.PublicationTimeInterval publicationTimeInterval = new PublicationDocument.PublicationTimeInterval();
        publicationTimeInterval.setV(dailyTimeInterval);
        publicationDocument.setPublicationTimeInterval(publicationTimeInterval);

        final PublicationDocument.Domain domain = new PublicationDocument.Domain();
        domain.setCodingScheme("A01");
        domain.setV(CeMergingConstants.CORE_REGION_ID);
        publicationDocument.setDomain(domain);

        final List<PublicationDocument.PublicationTimeSeries> publicationTimeSeries = computeAllTimeSeries(refProgResult,
                                                                                                     taskEntity,
                                                                                                     dailyTimeInterval,
                                                                                                     position);
        publicationDocument.getPublicationTimeSeries().addAll(publicationTimeSeries);
        return publicationDocument;
    }

    private List<PublicationDocument.PublicationTimeSeries> computeAllTimeSeries(final RefProgResult refProgResult,
                                                                                 final MergingTask taskEntity,
                                                                                 final String dailyTimeInterval,
                                                                                 final int position) {
        final List<PublicationDocument.PublicationTimeSeries> pubTimeSeriesList = new ArrayList<>();
        refProgResult.acExchanges().entrySet().forEach(entry -> pubTimeSeriesList.add(computePublicationTimeSeries(entry, CurrentType.AC, taskEntity, dailyTimeInterval, position)));
        refProgResult.virtualHubsExchanges().entrySet().forEach(entry -> pubTimeSeriesList.add(computePublicationTimeSeries(entry, CurrentType.DC, taskEntity, dailyTimeInterval, position)));
        return pubTimeSeriesList;
    }

    private PublicationDocument.PublicationTimeSeries computePublicationTimeSeries(final Map.Entry<Border, Double> entry,
                                                                                   final CurrentType currentType,
                                                                                   final MergingTask taskEntity,
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
        if (currentType.equals(CurrentType.AC)) {
            timeSeriesIdentif.setV(getAcTimeSeriesIdentification(taskEntity, entry));
            publTimeSeries.setTimeSeriesIdentification(timeSeriesIdentif);
        } else {
            timeSeriesIdentif.setV(getDcTimeSeriesIdentification(entry, taskEntity));
            publTimeSeries.setTimeSeriesIdentification(timeSeriesIdentif);
        }
        final PublicationDocument.PublicationTimeSeries.BusinessType businessType = new PublicationDocument.PublicationTimeSeries.BusinessType();
        businessType.setV("A66");
        publTimeSeries.setBusinessType(businessType);

        final PublicationDocument.PublicationTimeSeries.MeasureUnitQuantity measureUnitQuant = new PublicationDocument.PublicationTimeSeries.MeasureUnitQuantity();
        measureUnitQuant.setV("MAW");
        publTimeSeries.setMeasureUnitQuantity(measureUnitQuant);

        final PublicationDocument.PublicationTimeSeries.InArea inArea = new PublicationDocument.PublicationTimeSeries.InArea();
        inArea.setV(entry.getKey().getInArea());
        inArea.setCodingScheme("A01");
        publTimeSeries.setInArea(inArea);

        final PublicationDocument.PublicationTimeSeries.OutArea outArea = new PublicationDocument.PublicationTimeSeries.OutArea();
        outArea.setV(entry.getKey().getOutArea());
        outArea.setCodingScheme("A01");
        publTimeSeries.setOutArea(outArea);
        return publTimeSeries;
    }

    private String getAcTimeSeriesIdentification(final MergingTask taskEntity, final Map.Entry<Border, Double> entry) {
        final BiMap<String, String> allAreasBiMap = HashBiMap.create(taskEntity.getConfigurations().getRegionConfiguration().getAreasAll());
        final String countryTo = CountryCodeUtils.mapXkToKs(allAreasBiMap.inverse().get(entry.getKey().getInArea()));
        final String countryFrom = CountryCodeUtils.mapXkToKs(allAreasBiMap.inverse().get(entry.getKey().getOutArea()));
        return countryFrom + "-" + countryTo;
    }

    private String getDcTimeSeriesIdentification(final Map.Entry<Border, Double> entry, final MergingTask mergingTask) {
        final List<VirtualHubRecord> virtualHubList = mergingTask.getConfigurations().getVirtualHubList();

        final String eicCodeFrom = entry.getKey().getOutArea();
        final String eicCodeTo = entry.getKey().getInArea();

        final String countryEicCodeFrom = virtualHubList.stream()
            .filter(virtualHub -> virtualHub.getRelatedMaEic().equals(eicCodeFrom)).findFirst()
            .orElseThrow(() -> new CeMergingException("cannot find a virtualhub from country" + eicCodeFrom)).getRelatedMaCode();

        final String virtualHubEicCodeTo = virtualHubList.stream()
            .filter(virtualHub -> virtualHub.getEic().equals(eicCodeTo)).findFirst()
            .orElseThrow(() -> new CeMergingException("cannot find a virtualhub in direction to eicCode" + eicCodeTo)).getCode();

        return countryEicCodeFrom + "-" + virtualHubEicCodeTo;
    }
}

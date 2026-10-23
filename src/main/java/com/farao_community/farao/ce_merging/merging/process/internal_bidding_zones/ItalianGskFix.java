/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AreaType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.AutoNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.BusinessType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.CountryGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKDocument;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.GSKSeriesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.IdentificationType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualGSKBlockType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.ManualNodesType;
import com.farao_community.farao.ce_merging.xsd.glsk_fix.TimeIntervalType;

import java.math.BigDecimal;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.util.DateTimeUtils.toHourlyInterval;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.isItalianBiddingZoneEic;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.BusinessTypeList.Z_02;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.CodingSchemeType.A_01;
import static com.powsybl.iidm.network.Country.IT;
import static java.util.function.Predicate.not;

public class ItalianGskFix {
    public static byte[] handleItalianRegionsGlsk(final byte[] glskBytes,
                                                   final MergingTask task) {
        final GSKDocument glskDocument = JaxbUtils.readFromBytes(GSKDocument.class, glskBytes);

        final List<GSKSeriesType> series = glskDocument.getGSKSeries();

        final List<GSKSeriesType> itBzSeries = series
                .stream()
                .filter(serie -> isItalianBiddingZoneEic(serie.getArea().getV()))
                .toList();
        final GSKSeriesType itSerie = createItGsk(task, itBzSeries.getFirst().getTimeSeriesIdentification());

        series.removeAll(itBzSeries);

        if (hasCountryBlock(itBzSeries)) {
            itSerie.getCountryGSKBlock().add(createItCountryBlock(task));
        } else {
            final AutoGSKBlockType autoBlock = createItAutoBlock(task);
            itBzSeries.stream()
                    .map(GSKSeriesType::getAutoGSKBlock)
                    .flatMap(List::stream)
                    .map(AutoGSKBlockType::getAutoNodes)
                    .flatMap(List::stream)
                    .forEach(autoBlock.getAutoNodes()::add);

            itBzSeries.stream()
                    .map(GSKSeriesType::getManualGSKBlock)
                    .flatMap(List::stream)
                    .map(ManualGSKBlockType::getManualNodes)
                    .flatMap(List::stream)
                    .map(ItalianGskFix::fromManualNode)
                    .forEach(autoBlock.getAutoNodes()::add);

            itSerie.getAutoGSKBlock().add(autoBlock);
        }

        series.add(itSerie);

        return JaxbUtils.writeToBytes(GSKDocument.class, glskDocument);
    }

    private static AutoNodesType fromManualNode(final ManualNodesType manualNode) {
        final AutoNodesType autoNode = new AutoNodesType();
        autoNode.setNodeName(manualNode.getNodeName());
        return autoNode;
    }

    private static boolean hasCountryBlock(final List<GSKSeriesType> series) {
        return series.stream().map(GSKSeriesType::getCountryGSKBlock).anyMatch(not(List::isEmpty));
    }

    private static GSKSeriesType createItGsk(final MergingTask task,
                                             final IdentificationType id) {
        final String itaEic = task.getConfigurations().getRegionConfiguration().findEic(IT);

        final AreaType italy = new AreaType();
        italy.setCodingScheme(A_01);
        italy.setV(itaEic);
        final BusinessType businessType = new BusinessType();
        businessType.setV(Z_02);
        businessType.setShare(BigDecimal.valueOf(100.));

        final GSKSeriesType italySerie = new GSKSeriesType();
        italySerie.setBusinessType(businessType);
        italySerie.setTimeSeriesIdentification(id);
        italySerie.setArea(italy);

        return italySerie;
    }

    private static CountryGSKBlockType createItCountryBlock(final MergingTask task) {
        final TimeIntervalType interval = new TimeIntervalType();
        interval.setV(toHourlyInterval(task.getTargetDate()));
        final IdentificationType name = new IdentificationType();
        name.setV(IT.name());

        final CountryGSKBlockType italyBlock = new CountryGSKBlockType();
        italyBlock.setTimeInterval(interval);
        italyBlock.setGSKName(name);

        return italyBlock;
    }

    private static AutoGSKBlockType createItAutoBlock(final MergingTask task) {
        final TimeIntervalType interval = new TimeIntervalType();
        interval.setV(toHourlyInterval(task.getTargetDate()));
        final IdentificationType name = new IdentificationType();
        name.setV(IT.name());

        final AutoGSKBlockType italyBlock = new AutoGSKBlockType();
        italyBlock.setTimeInterval(interval);
        italyBlock.setGSKName(name);

        return italyBlock;
    }

}

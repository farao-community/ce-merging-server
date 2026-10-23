/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
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
import java.util.ArrayList;
import java.util.List;

import static com.farao_community.farao.ce_merging.xsd.glsk_fix.BusinessTypeList.Z_02;
import static com.farao_community.farao.ce_merging.xsd.glsk_fix.CodingSchemeType.A_01;

public class ItalianGskFixTestData {
    public static byte[] getInputWithoutCountryBlock() {
        // Create input GSK document with 5 Italian bidding zones
        final GSKDocument inputDocument = new GSKDocument();
        final List<GSKSeriesType> series = new ArrayList<>();

        int nodeCounter = 1;

        for (final ItalianBiddingZone zone : ItalianBiddingZone.values()) {
            final GSKSeriesType serie = createGskSerie(zone.getEic());

            // Add auto block with 2 nodes
            final AutoGSKBlockType autoBlock = new AutoGSKBlockType();
            final TimeIntervalType interval = new TimeIntervalType();
            interval.setV("2026-10-23T12:00Z/2026-10-23T13:00Z");
            autoBlock.setTimeInterval(interval);
            final IdentificationType name = new IdentificationType();
            name.setV(zone.name());
            autoBlock.setGSKName(name);

            final AutoNodesType autoNode1 = new AutoNodesType();
            autoNode1.setNodeName(createIdentificationType("AutoNode" + nodeCounter++));
            autoBlock.getAutoNodes().add(autoNode1);

            final AutoNodesType autoNode2 = new AutoNodesType();
            autoNode2.setNodeName(createIdentificationType("AutoNode" + nodeCounter++));
            autoBlock.getAutoNodes().add(autoNode2);

            serie.getAutoGSKBlock().add(autoBlock);

            // Add manual block with 1 node
            final ManualGSKBlockType manualBlock = new ManualGSKBlockType();
            manualBlock.setTimeInterval(interval);
            manualBlock.setGSKName(name);

            final ManualNodesType manualNode = new ManualNodesType();
            manualNode.setNodeName(createIdentificationType("ManualNode" + nodeCounter++));
            manualBlock.getManualNodes().add(manualNode);

            serie.getManualGSKBlock().add(manualBlock);

            series.add(serie);
        }

        inputDocument.getGSKSeries().addAll(series);
        return JaxbUtils.writeToBytes(GSKDocument.class, inputDocument);
    }

    public static byte[] getInputWithCountryBlock() {
        final GSKDocument inputDocument = new GSKDocument();
        final List<GSKSeriesType> series = new ArrayList<>();
        for (final ItalianBiddingZone zone : ItalianBiddingZone.values()) {
            final GSKSeriesType serie = createGskSerie(zone.getEic());
            final CountryGSKBlockType countryBlock = new CountryGSKBlockType();
            final TimeIntervalType interval = new TimeIntervalType();
            interval.setV("2026-10-23T12:00Z/2026-10-23T13:00Z");
            countryBlock.setTimeInterval(interval);
            final IdentificationType name = new IdentificationType();
            name.setV(zone.name());
            countryBlock.setGSKName(name);
            serie.getCountryGSKBlock().add(countryBlock);
            series.add(serie);
        }
        inputDocument.getGSKSeries().addAll(series);
        return JaxbUtils.writeToBytes(GSKDocument.class, inputDocument);
    }

    public static IdentificationType createIdentificationType(String value) {
        IdentificationType identificationType = new IdentificationType();
        identificationType.setV(value);
        return identificationType;
    }

    public static GSKSeriesType createGskSerie(String eic) {
        GSKSeriesType serie = new GSKSeriesType();

        AreaType area = new AreaType();
        area.setCodingScheme(A_01);
        area.setV(eic);
        serie.setArea(area);

        BusinessType businessType = new BusinessType();
        businessType.setV(Z_02);
        businessType.setShare(BigDecimal.valueOf(100.));
        serie.setBusinessType(businessType);

        IdentificationType id = new IdentificationType();
        id.setV("TestSerie");
        serie.setTimeSeriesIdentification(id);

        return serie;
    }
}

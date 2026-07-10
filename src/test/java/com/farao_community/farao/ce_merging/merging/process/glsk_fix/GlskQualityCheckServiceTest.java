/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.glsk_fix;

import com.farao_community.farao.ce_merging.xsd.glsk_fix.*;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;
import static com.farao_community.farao.ce_merging.merging.process.glsk_fix.GlskQualityCheckService.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
public class GlskQualityCheckServiceTest {

    private static final String ALEGRO_FILE = "20160728_Alegro.xml";
    private static final String VALID_DATE = "2016-07-28T23:30:00Z";
    private static final String NODE_NAME = "NODE_1";
    public static final String GLSK_NODE_NOT_FOUND = "glsk.node.not.found";

    @Autowired
    private GlskQualityCheckService glskQualityCheckService;

    @Test
    public void shouldReturnAlegroSeries() throws IOException, URISyntaxException {
        final List<GSKSeriesType> alegroGskSeries = glskQualityCheckService.getAlegroGskSeries(
                loadGlsk(ALEGRO_FILE),
                instant(VALID_DATE)
        );
        assertEquals(2, alegroGskSeries.size());
    }

    @Test
    public void shouldReturnEmptyListWhenTargetDateDoesNotMatch() throws IOException, URISyntaxException {
        final List<GSKSeriesType> alegroGskSeries = glskQualityCheckService.getAlegroGskSeries(
                loadGlsk(ALEGRO_FILE),
                instant("2019-08-29T23:30:00Z")
        );
        assertTrue(alegroGskSeries.isEmpty());
    }

    @Test
    public void shouldReturnEmptyListWhenFileContainsNoAlegroSeries() throws IOException, URISyntaxException {
        final List<GSKSeriesType> alegroGskSeries = glskQualityCheckService.getAlegroGskSeries(
                loadGlsk("20160728_GLSK.xml"),
                instant(VALID_DATE));
        assertTrue(alegroGskSeries.isEmpty());
    }

    @Test
    public void shouldDeleteALegroQualityLogs() {
        final ReportNode rootReportNode = createReport();
        addReport(rootReportNode, "glsk.node.not.found", "XLI_OB1A", "Load", "22Y201903145---4");
        addReport(rootReportNode, "glsk.node.connected", "XLI_OB1B", "Generator", "22Y201903144---9");
        addReport(rootReportNode, "glsk.node.not.found", "XNODE", "Load", "NL");
        final ReportNode newReporter = glskQualityCheckService.removeInitialAlegroReports(rootReportNode);
        assertEquals(1, newReporter.getChildren().size());
    }

    @Test
    void shouldReportNodeNotFound() {
        final GSKSeriesType series = createSeriesWithNode(NODE_NAME);
        final Network network = mock(Network.class);
        when(network.getDanglingLineStream()).thenReturn(Stream.empty());
        final ReportNode reportNode = createReport();
        glskQualityCheckService.checkAlegroGSKSeries(series, network, reportNode);
        assertEquals(1, reportNode.getChildren().size());
        final ReportNode child = reportNode.getChildren().getFirst();
        assertEquals(GLSK_NODE_NOT_FOUND, child.getMessageKey());
        assertEquals("GLSK node is not found in CGM", child.getMessageTemplate());
    }

    @Test
    void shouldReportDisconnectedNode() {
        final ReportNode reportNode = createReport();
        final GSKSeriesType series = createSeriesWithNode(NODE_NAME);
        final Network network = createNetwork(false);
        glskQualityCheckService.checkAlegroGSKSeries(series, network, reportNode);
        assertEquals(1, reportNode.getChildren().size());
        assertEquals("glsk.node.connected", reportNode.getChildren().getFirst().getMessageKey());
    }

    @Test
    void shouldNotReportAnythingWhenNodeIsConnected() {
        final ReportNode reportNode = createReport();
        final GSKSeriesType series = createSeriesWithNode(NODE_NAME);
        final Network network = createNetwork(true);
        glskQualityCheckService.checkAlegroGSKSeries(series, network, reportNode);
        assertTrue(reportNode.getChildren().isEmpty());
    }

    private Network createNetwork(final boolean connected) {
        final Terminal terminal = mock(Terminal.class);
        when(terminal.isConnected()).thenReturn(connected);

        final DanglingLine danglingLine = mock(DanglingLine.class);
        when(danglingLine.getPairingKey()).thenReturn(NODE_NAME);
        when(danglingLine.getTerminal()).thenReturn(terminal);

        final Network network = mock(Network.class);
        when(network.getDanglingLineStream()).thenReturn(Stream.of(danglingLine));

        return network;
    }

    private void addReport(final ReportNode reportNode,
                           final String messageKey,
                           final String nodeName,
                           final String type,
                           final String tso) {
        reportNode.newReportNode()
                .withResourceBundles(REPORT_BASE_NAME)
                .withMessageTemplate(messageKey)
                .withTypedValue(NODE_ID_KEY, nodeName, "")
                .withTypedValue(TYPE_KEY, type, "")
                .withTypedValue(TSO_KEY, tso, "")
                .withSeverity(TypedValue.WARN_SEVERITY)
                .add();
    }

    private GSKSeriesType createSeriesWithNode(final String nodeName) {
        final IdentificationType name = new IdentificationType();
        name.setV(nodeName);
        final AutoNodesType autoNode = new AutoNodesType();
        autoNode.setNodeName(name);
        final AutoGSKBlockType block = new AutoGSKBlockType();
        block.getAutoNodes().add(autoNode);
        final GSKSeriesType series = new GSKSeriesType();
        series.getAutoGSKBlock().add(block);
        final BusinessType businessType = new BusinessType();
        businessType.setV(BusinessTypeList.Z_02);
        series.setBusinessType(businessType);
        final AreaType area = new AreaType();
        area.setV("AREA");
        series.setArea(area);
        return series;
    }

    private ReportNode createReport() {
        return ReportNode.newRootReportNode()
                .withResourceBundles(REPORT_BASE_NAME)
                .withMessageTemplate("glsk.quality.report")
                .build();
    }

    private byte[] loadGlsk(String fileName) throws IOException, URISyntaxException {
        return Files.readAllBytes(
                Paths.get(getClass().getResource("/glskFix/" + fileName).toURI()));
    }

    private Instant instant(String isoDate) {
        return OffsetDateTime.parse(isoDate).toInstant();
    }
}
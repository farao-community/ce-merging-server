/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones;

import com.farao_community.farao.ce_merging.common.util.JaxbUtils;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.common.util.NetworkUtil;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BiddingZoneUCTEDocument;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.BranchType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.InternalTieLineType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.InternalTieLinesType;
import com.farao_community.farao.ce_merging.xsd.internal_bidding_zones.TextOnlyType;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import test_utils.CeTestUtils;

import java.util.List;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.JaxbUtils.readFromPath;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getLineFlow;
import static com.farao_community.farao.ce_merging.common.util.NetworkUtil.getVlIdOnSide;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.ICNO;
import static com.farao_community.farao.ce_merging.merging.process.internal_bidding_zones.ItalianBiddingZone.INOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.mockLine;
import static test_utils.CeTestUtils.mockSavedFile;

class InternalBiddingZonesManagerTest {

    private static final String LINE_1 = "LINE_1";
    private static final String LINE_2 = "LINE_2";
    private static final String NODE_A = "NODE_A";
    private static final String NODE_B = "NODE_B";
    private static final String NODE_C = "NODE_C";
    private static final String NODE_D = "NODE_D";
    private static final String CGM_PATH = "path/to/cgm";
    private static final String BZ_PATH = "path/to/ita_bz";
    private static final double A_TO_B_FLOW = 100;
    private static final double C_TO_D_FLOW = 50;

    @Test
    void shouldSumFlowsOnInternalTieLines() {
        final BiddingZoneUCTEDocument bzDoc = mockBiddingZoneDoc();

        // Mock Network with same lines as BZ doc
        final Network network = mock(Network.class);
        final Line line1 = mockLine(LINE_1, NODE_A, NODE_B, A_TO_B_FLOW);
        final Line line2 = mockLine(LINE_2, NODE_C, NODE_D, C_TO_D_FLOW);
        doAnswer(call -> Stream.of(line1, line2))
                .when(network).getLineStream();
        try (final MockedStatic<JaxbUtils> jaxbUtils = mockStatic(JaxbUtils.class);
             final MockedStatic<Network> networkStatic = mockStatic(Network.class);
             final MockedStatic<LoadFlowUtils> loadFlowUtils = mockStatic(LoadFlowUtils.class);
             final MockedStatic<NetworkUtil> networkUtil = mockStatic(NetworkUtil.class)) {

            jaxbUtils.when(() -> readFromPath(BiddingZoneUCTEDocument.class, BZ_PATH))
                    .thenReturn(bzDoc);
            networkStatic.when(() -> Network.read(CGM_PATH)).thenReturn(network);
            loadFlowUtils.when(() -> runLoadFlow(any(), any(), any(LoadFlowParameters.class)))
                    .thenAnswer(invocation -> null);
            networkUtil.when(() -> getVlIdOnSide(any(), any())).thenCallRealMethod();
            networkUtil.when(() -> getLineFlow(line1))
                    .thenReturn(100.0);
            networkUtil.when(() -> getLineFlow(line2))
                    .thenReturn(50.0);

            final InternalBiddingZonesManager manager = new InternalBiddingZonesManager(getTask(),
                                                                                        CeTestUtils::mockLoadFlowRunner);

            // Expected: sum of flows on both lines
            assertEquals(A_TO_B_FLOW + C_TO_D_FLOW, manager.getTotalFlow(INOR, ICNO), 0.01);
        }
    }

    private BiddingZoneUCTEDocument mockBiddingZoneDoc() {
        final BiddingZoneUCTEDocument biddingZoneDoc = mock(BiddingZoneUCTEDocument.class);
        final InternalTieLinesType internalTieLines = mock(InternalTieLinesType.class);
        when(biddingZoneDoc.getInternalTieLines()).thenReturn(internalTieLines);

        // Create two "tie lines" from INOR to ICNO
        when(internalTieLines.getInternalTieLine()).thenReturn(List.of(mockItalianTieLine(LINE_1, NODE_A, NODE_B),
                                                                       mockItalianTieLine(LINE_2, NODE_C, NODE_D)));
        return biddingZoneDoc;
    }

    private MergingTask getTask() {
        final MergingTask task = new MergingTask();
        final Inputs inputs = new Inputs();
        inputs.setInternalBiddingZones(mockSavedFile(BZ_PATH));
        final Outputs outputs = new Outputs();
        outputs.setCgm(mockSavedFile(CGM_PATH));
        task.setInputs(inputs);
        final Configurations configurations = new Configurations();
        configurations.setLoadFlowParameters(new LoadFlowParameters());
        task.setOutputs(outputs);
        task.setConfigurations(configurations);
        return task;
    }

    private InternalTieLineType mockItalianTieLine(final String elementName,
                                                   final String nodeFrom,
                                                   final String nodeTo) {
        final InternalTieLineType tieLine = new InternalTieLineType();
        final TextOnlyType bzFrom = new TextOnlyType();
        final TextOnlyType bzTo = new TextOnlyType();
        final BranchType branch = new BranchType();

        bzFrom.setValue(INOR.name());
        bzTo.setValue(ICNO.name());
        tieLine.setBiddingZoneFrom(bzFrom);
        tieLine.setBiddingZoneTo(bzTo);
        tieLine.setBranch(branch);
        branch.setElementName(elementName);
        branch.setFrom(nodeFrom);
        branch.setTo(nodeTo);

        return tieLine;
    }

}

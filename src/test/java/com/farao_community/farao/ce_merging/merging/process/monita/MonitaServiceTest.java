package com.farao_community.farao.ce_merging.merging.process.monita;

import com.farao_community.farao.ce_merging.common.model.netpositions.GenerationAndLoadQuantity;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsValues;
import com.farao_community.farao.ce_merging.common.util.BordersUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.function.Predicates;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.BordersUtils.isBorderOf;
import static com.powsybl.iidm.network.Country.IT;
import static com.powsybl.iidm.network.Country.ME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class MonitaServiceTest {

    private static final String MONITA1_ME_NODE_NAME = "XKOTR120";
    private static final String MONITA2_ME_NODE_NAME = "XKOTR220";

    @Test
    void shouldMoveMonitaVirtualHubsFromItalyToMontenegro() {
        final MergingTask task = new MergingTask();
        final Inputs inputs = new Inputs();
        final IgmData itIgm = new IgmData();
        itIgm.setIgmFilePath("20260720_1843_FO1_IT1.uct");
        inputs.setIgms(List.of(itIgm));
        task.setInputs(inputs);
        final Artifacts artifacts = new Artifacts();
        task.setArtifacts(artifacts);

        Map<String, NetPositions> netPositionsMap = new HashMap<>();
        
        // Italy NetPositions
        Map<String, Double> itVirtualHubs = new HashMap<>();
        itVirtualHubs.put(MONITA1_ME_NODE_NAME, 100.0);
        itVirtualHubs.put(MONITA2_ME_NODE_NAME, 200.0);
        NetPositions itNp = new NetPositions(
                new NetPositionsValues(300.0, 0.0),
                new NetPositionsValues(0.0, 0.0),
                300.0,
                itVirtualHubs,
                new HashMap<>(),
                new GenerationAndLoadQuantity(0.0, 0.0)
        );
        netPositionsMap.put(IT.name(), itNp);

        // Montenegro NetPositions
        NetPositions meNp = new NetPositions(
                new NetPositionsValues(500.0, 500.0),
                new NetPositionsValues(500.0, 500.0),
                500.0,
                new HashMap<>(),
                new HashMap<>(),
                new GenerationAndLoadQuantity(0.0, 0.0)
        );
        netPositionsMap.put(ME.name(), meNp);

        NetPositionsResults results = new NetPositionsResults(netPositionsMap);

        try (MockedStatic<Network> networkStatic = mockStatic(Network.class);
             MockedStatic<BordersUtils> bordersUtils = mockStatic(BordersUtils.class)) {
            // given :
            final Network network = mock(Network.class);
            final DanglingLine monita1 = mock(DanglingLine.class);
            final DanglingLine monita2 = mock(DanglingLine.class);

            networkStatic.when(() -> Network.read(anyString())).thenReturn(network);
            when(monita1.getPairingKey()).thenReturn(MONITA1_ME_NODE_NAME);
            when(monita1.getP0()).thenReturn(100.0);
            when(monita2.getPairingKey()).thenReturn(MONITA2_ME_NODE_NAME);
            when(monita2.getP0()).thenReturn(200.0);
            bordersUtils.when(() -> isBorderOf(IT)).thenReturn(Predicates.truePredicate());
            when(network.getDanglingLineStream()).thenAnswer(i -> Stream.of(monita1, monita2));

            // when :
            MonitaService.postTreatmentForMonita(task, results);

            // then :
            assertEquals(0, itNp.getGlobalNetPosition().getWithVirtualHubs());
            assertEquals(0, itNp.getOutBciNetPosition());
            assertFalse(itNp.getVirtualHubsExchanges().containsKey(MONITA1_ME_NODE_NAME));
            assertFalse(itNp.getVirtualHubsExchanges().containsKey(MONITA2_ME_NODE_NAME));

            assertEquals(800, meNp.getGlobalNetPosition().getWithVirtualHubs());
            assertEquals(800, meNp.getOutBciNetPosition());
            assertEquals(100, meNp.getVirtualHubFlow(MONITA1_ME_NODE_NAME));
            assertEquals(200, meNp.getVirtualHubFlow(MONITA2_ME_NODE_NAME));
        }
    }
}
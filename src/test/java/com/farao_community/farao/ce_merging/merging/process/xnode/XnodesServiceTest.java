/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.entity.XnodeConfig;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.model.hourly.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.VoltageLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.merging.model.hourly.enums.ArtifactType.XNODES_INFORMATION_FILE;
import static com.farao_community.farao.ce_merging.merging.model.hourly.enums.GermanTso.D2;
import static com.farao_community.farao.ce_merging.merging.model.hourly.enums.GermanTso.D4;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.CLOSE;
import static com.farao_community.farao.ce_merging.merging.process.xnode.XnodeStatus.OPEN;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class XnodesServiceTest {

    private static final String XDE_NODE = "XDE_NODE";
    private static final String XFR_NODE = "XFR_NODE";

    private XnodesService xnodesService;
    private MergingTaskRepository tasksRepository;
    private CeMergingConfiguration configuration;
    private InitialImportService initialImportService;

    @BeforeEach
    void setUp() {
        tasksRepository = mock(MergingTaskRepository.class);
        configuration = mock(CeMergingConfiguration.class);
        initialImportService = mock(InitialImportService.class);
        final XnodesCalculation xnodesCalculation = new XnodesCalculation();
        xnodesService = new XnodesService(tasksRepository, configuration, initialImportService, xnodesCalculation);
    }

    @Test
    void shouldCheckIgmsStatus() {
        final MergingTask task = new MergingTask();
        final Configurations configurations = new Configurations();
        task.setConfigurations(configurations);

        configurations.setXnodeList(List.of(new XnodeConfig(XDE_NODE, DE.name(), D2.name(), DE.name(), D4.name()),
                                            new XnodeConfig(XFR_NODE, FR.name(), null, DE.name(), D2.name())));

        final Network germanNetwork = mock(Network.class);
        when(germanNetwork.getNameOrId()).thenReturn("german_net");
        final DanglingLine germanDl = mock(DanglingLine.class);
        when(germanDl.getPairingKey()).thenReturn(XDE_NODE);
        final Terminal germanTerminal = mock(Terminal.class);
        when(germanDl.getTerminal()).thenReturn(germanTerminal);
        when(germanTerminal.isConnected()).thenReturn(true);
        final Terminal.BusBreakerView germanBbv = mock(Terminal.BusBreakerView.class);
        when(germanTerminal.getBusBreakerView()).thenReturn(germanBbv);
        final VoltageLevel germanVl = mock(VoltageLevel.class);
        final Substation germanSubstation = mock(Substation.class);
        when(germanSubstation.getNullableCountry()).thenReturn(DE);
        when(germanVl.getSubstation()).thenReturn(Optional.of(germanSubstation));
        when(germanTerminal.getVoltageLevel()).thenReturn(germanVl);
        when(germanVl.getId()).thenReturn("GERMAN_VL");
        when(germanDl.getP0()).thenReturn(10.0);
        when(germanDl.getQ0()).thenReturn(5.0);
        when(germanNetwork.getDanglingLineStream()).thenAnswer(invocation -> Stream.of(germanDl));

        final Network frNetwork = mock(Network.class);
        when(frNetwork.getNameOrId()).thenReturn("fr_net");
        final DanglingLine frDl = mock(DanglingLine.class);
        when(frDl.getPairingKey()).thenReturn(XFR_NODE);
        final Terminal frTerminal = mock(Terminal.class);
        when(frDl.getTerminal()).thenReturn(frTerminal);
        when(frTerminal.isConnected()).thenReturn(false);
        final Terminal.BusBreakerView frBbv = mock(Terminal.BusBreakerView.class);
        when(frTerminal.getBusBreakerView()).thenReturn(frBbv);
        final VoltageLevel frVl = mock(VoltageLevel.class);
        final Substation frSubstation = mock(Substation.class);
        when(frSubstation.getNullableCountry()).thenReturn(FR);
        when(frVl.getSubstation()).thenReturn(Optional.of(frSubstation));
        when(frTerminal.getVoltageLevel()).thenReturn(frVl);
        when(frVl.getId()).thenReturn("FR_VL");
        when(frDl.getP0()).thenReturn(20.0);
        when(frDl.getQ0()).thenReturn(10.0);
        when(frNetwork.getDanglingLineStream()).thenAnswer(invocation -> Stream.of(frDl));

        when(initialImportService.importInitialIgms(task)).thenReturn(Map.of(D2.name(), germanNetwork,
                                                                             FR.name(), frNetwork));

        try (final MockedStatic<FileStorageUtils> fileStorageUtilsMock = mockStatic(FileStorageUtils.class)) {

            xnodesService.checkIgmsStatus(task);

            final ArgumentCaptor<Map<String, XnodeInformation>> captor = ArgumentCaptor.forClass(Map.class);
            fileStorageUtilsMock.verify(() -> FileStorageUtils.saveArtifactFile(eq(XNODES_INFORMATION_FILE), captor.capture(), eq(task), eq(configuration)));

            final Map<String, XnodeInformation> result = captor.getValue();
            assertEquals(2, result.size());

            final XnodeInformation germanInfo = result.get(XDE_NODE);
            assertNotNull(germanInfo);
            assertEquals(D2.name(), germanInfo.getArea1Information().getCountry());
            assertEquals(CLOSE, germanInfo.getArea1Information().getStatus());
            assertEquals("GERMAN_VL", germanInfo.getArea1Information().getNode());
            assertEquals(10.0, germanInfo.getArea1Information().getP(), 0.01);

            final XnodeInformation frInfo = result.get(XFR_NODE);
            assertNotNull(frInfo);
            assertEquals(FR.name(), frInfo.getArea1Information().getCountry());
            assertEquals(OPEN, frInfo.getArea1Information().getStatus());
            assertEquals("FR_VL", frInfo.getArea1Information().getNode());
            assertEquals(20.0, frInfo.getArea1Information().getP(), 0.01);
        }

        verify(tasksRepository).save(task);
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.german_pre_merge;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.model.netpositions.GenerationAndLoadQuantity;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsValues;
import com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import test_utils.TaskTestUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.farao_community.farao.ce_merging.common.util.StreamsUtils.sumProperty;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_IGMS_NET_POSITIONS_FILE;
import static com.powsybl.iidm.network.Country.DE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static test_utils.CeTestUtils.stringPathOf;

@SpringBootTest
@ActiveProfiles("OpenLoadFlow")
class GermanMismatchCompensationServiceTest {
    private static final double VALUE_TOLERANCE = 1;
    @Autowired
    private GermanMismatchCompensationService service;

    @Autowired
    private Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    private MergingTask task;
    private Configurations configurations;

    @Autowired
    private CeMergingConfiguration configuration;

    @BeforeEach
    void setUp() throws IOException {

        final List<IgmData> germanIgms = GermanTso.stream()
                .map(GermanTso::name)
                .map(tso -> {
                    final IgmData igm = new IgmData();
                    igm.setCountry(tso);
                    igm.setIgmFilePath(stringPathOf("netPositionsData/20190618_0030_2D2_" + tso + "1.uct"));
                    return igm;
                }).toList();

        final Inputs inputs = new Inputs();
        inputs.setTargetDate(OffsetDateTime.of(2019, 6, 18, 1, 30, 0, 0, ZoneOffset.UTC));

        inputs.setIgms(germanIgms);

        task = new MergingTask();
        task.setId(1L);
        task.setInputs(inputs);
        task.setArtifacts(new Artifacts());

        TaskTestUtils.setTaskDefaultConfigurations(task);
        configurations = task.getConfigurations();

        final String loadflowParametersFile = "loadflow_parameters/ac-load-flow-parameters_main_component.json";
        TaskTestUtils.setLoadflowParameters(task, loadflowParametersFile);

        Files.createDirectories(Paths.get(configuration.getOutputsDirectoryPath(task)));
        Files.createDirectories(Paths.get(configuration.getArtifactsDirectoryPath(task)));
    }

    @Test
    void shouldCompensateGermanMismatch() throws FileNotFoundException {
        final Network mergedNetwork = GermanPreMergeService.mergeGermanRegions(task);
        service.apply(task, mergedNetwork);
        checkInitialGermanNetPositions(task);

        loadFlowRunnerSupplier.get().run(mergedNetwork, configurations.getLoadFlowParameters());
        final NetPositions germanNetPositions = computeGermanNetPositions(mergedNetwork);
        assertNotNull(germanNetPositions);
        final GenerationAndLoadQuantity generationAndLoad = germanNetPositions.getGenerationAndLoadQuantity();
        final NetPositionsValues globalNetPosition = germanNetPositions.getGlobalNetPosition();

        assertEquals(-10, generationAndLoad.generation(), VALUE_TOLERANCE);
        assertEquals(1000, generationAndLoad.load(), VALUE_TOLERANCE);
        assertEquals(510, globalNetPosition.getWithoutVirtualHubs(), VALUE_TOLERANCE);
        assertEquals(510, globalNetPosition.getWithVirtualHubs(), VALUE_TOLERANCE);
    }

    private NetPositions computeGermanNetPositions(final Network mergedNetwork) {
        return CountryNetPositionHandler.computeCountryNetPositions(DE, mergedNetwork, configurations);
    }

    private void checkInitialGermanNetPositions(final MergingTask task) throws FileNotFoundException {
        final NetPositionsResults results = task.getArtifact(GERMAN_IGMS_NET_POSITIONS_FILE, NetPositionsResults.class);
        final double sumIntNetPositions = service.getInitialInternalNetPosition(results);
        final double sumExtNetPositions = getInitialExternalNetPosition(results);
        final double sumInitialNetPositionWithoutVirtualHubs = sumExtNetPositions + sumIntNetPositions;
        final double otherWayToSumInitialNpWoVh = results.netPositionsByCountryMap().values()
                .stream().mapToDouble(netPositions -> netPositions.getGlobalNetPosition()
                        .getWithoutVirtualHubs()).sum();

        assertEquals(0, sumIntNetPositions, VALUE_TOLERANCE);
        assertEquals(3820, sumExtNetPositions, VALUE_TOLERANCE);
        assertEquals(3820, sumInitialNetPositionWithoutVirtualHubs, VALUE_TOLERANCE); //values from convergence
        assertEquals(otherWayToSumInitialNpWoVh, sumInitialNetPositionWithoutVirtualHubs, VALUE_TOLERANCE);

        final double sumInitialGeneration = sumProperty(getInjectionValues(results), GenerationAndLoadQuantity::generation);
        final double sumInitialLoad = sumProperty(getInjectionValues(results), GenerationAndLoadQuantity::load);

        assertEquals(-40, sumInitialGeneration, VALUE_TOLERANCE);
        assertEquals(1580, sumInitialLoad, VALUE_TOLERANCE);
    }

    private Stream<GenerationAndLoadQuantity> getInjectionValues(final NetPositionsResults results) {
        return GermanTso.stream()
                .map(GermanTso::name)
                .map(results::get)
                .filter(Objects::nonNull)
                .map(NetPositions::getGenerationAndLoadQuantity);
    }

    private double getInitialExternalNetPosition(final NetPositionsResults germanNetPositionResults) {
        return GermanTso.stream()
                .map(GermanTso::name)
                .map(germanNetPositionResults::get)
                .filter(Objects::nonNull)
                .mapToDouble(res -> res.getGlobalNetPosition().getWithoutVirtualHubs()
                                    - res.getGlobalDetailedExchanges().getOrDefault(DE.name(), 0.))
                .sum();
    }

}

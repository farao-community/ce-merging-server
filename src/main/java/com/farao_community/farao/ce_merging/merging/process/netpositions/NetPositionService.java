/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.netpositions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositions;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.process.monita.MonitaService;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.entsoe.util.EntsoeGeographicalCode;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.util.LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection;
import static com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler.initHandler;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;
import static java.util.stream.Collectors.toMap;

@Service
public class NetPositionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetPositionService.class);

    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    public NetPositionService(final MergingTaskRepository tasksRepository,
                              final CeMergingConfiguration configuration,
                              final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }

    public void computeInitialNetPositions(final MergingTask task) {
        try {
            final Artifacts artifacts = task.getArtifacts();
            final Map<String, NetPositions> fromPreTreatedInputs = task.getInputs()
                    .getIgms()
                    .stream()
                    .filter(igmData -> isNotPreTreated(task, igmData.getCountry()))
                    .collect(toMap(IgmData::getCountry,
                                   igmData -> computeNetPositions(task, igmData.getIgmFile(), igmData.getCountry())));

            final NetPositionsResults netPositionsFile = new NetPositionsResults(fromPreTreatedInputs);

            netPositionsFile.put(DE, computeNetPositions(task, artifacts.getFile(GERMAN_PRE_MERGED_IGM), DE.name()));
            netPositionsFile.put(DK, computeNetPositions(task, artifacts.getFile(DK_CONVERTED_FILE), DK.name()));

            netPositionsFile.netPositionsByCountryMap()
                    .putAll(artifacts.getPreTreatedIgmMap()
                                    .entrySet()
                                    .stream()
                                    .collect(toMap(Map.Entry::getKey,
                                                   e -> computeNetPositions(task, e.getValue(), e.getKey()))));

            MonitaService.postTreatmentForMonita(task, netPositionsFile);

            FileStorageUtils.saveArtifactFile(IGMS_NET_POSITIONS_FILE, netPositionsFile, task, configuration);
            tasksRepository.save(task);
        } catch (Exception e) {
            String errorMessage = String.format("Initial net positions calculation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private NetPositions computeNetPositions(final MergingTask task,
                                             final SavedFile savedNetwork,
                                             final String countryCode) {
        final Network network = prepareNetwork(task, savedNetwork);
        final Country country = EntsoeGeographicalCode.valueOf(countryCode).getCountry();

        final CountryNetPositionHandler handler = initHandler(country, network, task.getConfigurations());
        return handler.computeNetPositions();
    }

    private Network prepareNetwork(final MergingTask task,
                                   final SavedFile savedNetwork) {
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        final Network network = Network.read(savedNetwork.getPath());
        runLoadFlowWithBalanceTypeCorrection(network, loadFlowRunnerSupplier, loadFlowParameters);
        return network;
    }

    private static boolean isNotPreTreated(final MergingTask task,
                                           final String areaCode) {
        final Set<String> preTreated = task.getArtifacts().getPreTreatedIgmMap().keySet();
        return !DANISH_TSO.equals(areaCode) && !GermanTso.includes(areaCode) && !preTreated.contains(areaCode);
    }

    public NetPositionsResults computeGermanNetPositions(final MergingTask task) {
        final LoadFlowParameters lfParams = task.getConfigurations().getLoadFlowParameters();
        final Map<String, NetPositions> result = task.getInputs()
                .getIgms()
                .stream()
                .filter(igmData -> GermanTso.includes(igmData.getCountry()))
                .collect(toMap(IgmData::getCountry,
                               igmData -> computeGermanTsoNetPositions(task, igmData.getIgmFile(), lfParams)));
        return new NetPositionsResults(result);
    }

    private NetPositions computeGermanTsoNetPositions(final MergingTask task,
                                                      final SavedFile igmFile,
                                                      final LoadFlowParameters loadFlowParameters) {
        final Network network = Network.read(igmFile.getPath());
        runLoadFlowWithBalanceTypeCorrection(network, loadFlowRunnerSupplier, loadFlowParameters);
        final NetPositionsResults netPositionsFile = new NetPositionsResults(
                Map.of(DE.name(), initHandler(DE, network, task.getConfigurations()).computeNetPositions())
        );
        return netPositionsFile.netPositionsByCountryMap().getOrDefault("DE", null);
    }

}

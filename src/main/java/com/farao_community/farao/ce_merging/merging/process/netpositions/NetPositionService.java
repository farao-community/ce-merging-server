/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.netpositions;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.model.netpositions.NetPositionsResults;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.common.util.LoadFlowUtils;
import com.farao_community.farao.ce_merging.merging.process.monita.MonitaService;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
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

import java.util.Set;
import java.util.function.Supplier;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DANISH_TSO;
import static com.farao_community.farao.ce_merging.merging.process.netpositions.CountryNetPositionHandler.buildFrom;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.IGMS_NET_POSITIONS_FILE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.DK;

@Service
public class NetPositionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetPositionService.class);

    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;
    private final Supplier<LoadFlow.Runner> loadFlowRunnerSupplier;

    public NetPositionService(MergingTaskRepository tasksRepository,
                              CeMergingConfiguration configuration,
                              Supplier<LoadFlow.Runner> loadFlowRunnerSupplier) {
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
        this.loadFlowRunnerSupplier = loadFlowRunnerSupplier;
    }

    public void computeInitialNetPositions(MergingTask task) {
        try {
            final NetPositionsResults netPositionsFile = new NetPositionsResults();

            task.getInputs()
                    .getIgms()
                    .stream()
                    .filter(igmData -> isNotPreTreated(task, igmData.getCountry()))
                    .forEach(igmData -> computeNetPositions(task, igmData.getIgmFile(), igmData.getCountry(), netPositionsFile));

            computeNetPositions(task, task.getArtifacts().getFile(GERMAN_PRE_MERGED_IGM), DE.name(), netPositionsFile);
            computeNetPositions(task, task.getArtifacts().getFile(DK_CONVERTED_FILE), DK.name(), netPositionsFile);

            task.getArtifacts()
                    .getPreTreatedIgmMap()
                    .forEach((country, savedFile) ->
                                     computeNetPositions(task, savedFile, country, netPositionsFile));

            MonitaService.postTreatmentForMonita(task, netPositionsFile);

            FileStorageUtils.saveArtifactFile(IGMS_NET_POSITIONS_FILE, netPositionsFile, task, configuration);
            tasksRepository.save(task);
        } catch (Exception e) {
            String errorMessage = String.format("Initial net positions calculation failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private void computeNetPositions(MergingTask task,
                                     SavedFile savedNetwork,
                                     String countryCode,
                                     NetPositionsResults netPositionsFile) {
        final Network network = prepareNetwork(task, savedNetwork);
        final Country country = EntsoeGeographicalCode.valueOf(countryCode).getCountry();

        final CountryNetPositionHandler countryNetPositionHandler = buildFrom(country, network, task.getConfigurations());
        netPositionsFile.put(country, countryNetPositionHandler.computeNetPositions());
    }

    private Network prepareNetwork(final MergingTask task,
                                   final SavedFile savedNetwork) {
        final LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        final Network network = Network.read(savedNetwork.getPath());
        LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection(network, loadFlowRunnerSupplier, loadFlowParameters);
        return network;
    }

    private static boolean isNotPreTreated(final MergingTask task,
                                           final String areaCode) {
        final Set<String> preTreated = task.getArtifacts().getPreTreatedIgmMap().keySet();
        return !DANISH_TSO.equals(areaCode) && !GermanTso.includes(areaCode) && !preTreated.contains(areaCode);
    }

    public NetPositionsResults computeGermanNetPositions(MergingTask task) {
        LoadFlowParameters loadFlowParameters = task.getConfigurations().getLoadFlowParameters();
        NetPositionsResults germanNetPositionsFile = new NetPositionsResults();
        task.getInputs().getIgms().stream()
                .filter(igmData -> GermanTso.includes(igmData.getCountry()))
                .forEach(igmData -> computeGermanTsoNetPositions(task, igmData.getIgmFile(), igmData.getCountry(), germanNetPositionsFile, loadFlowParameters));
        return germanNetPositionsFile;
    }

    private void computeGermanTsoNetPositions(MergingTask task,
                                              SavedFile igmFile,
                                              String tso,
                                              NetPositionsResults germanNetPositionsFile,
                                              LoadFlowParameters loadFlowParameters) {
        try {
            Network network = Network.read(igmFile.getPath());
            LoadFlowUtils.runLoadFlowWithBalanceTypeCorrection(network, loadFlowRunnerSupplier, loadFlowParameters);
            NetPositionsResults netPositionsFile = new NetPositionsResults();
            netPositionsFile.put(DE, buildFrom(DE, network, task.getConfigurations()).computeNetPositions());
            germanNetPositionsFile.putIfAbsent(tso, netPositionsFile.netPositionsByCountryMap().getOrDefault("DE", null));
        } catch (Exception e) {
            String errorMessage = "Error occurred while computing net position for tso: " + tso + ", cause: " + e.getMessage();
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

}

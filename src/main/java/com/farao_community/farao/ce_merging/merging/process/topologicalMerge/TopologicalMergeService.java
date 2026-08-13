/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.topologicalMerge;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.ImportConfig;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.GERMAN_AND_DANISH_TSO;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;

@Service
public class TopologicalMergeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopologicalMergeService.class);
    private static final String UCTE_IMPORT_CREATE_AREAS_KEY = "ucte.import.create-areas";
    private final CeMergingConfiguration configuration;

    public TopologicalMergeService(CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void mergeInitialIgms(final MergingTask task) {
        try {
            final Network mergedNetwork = getTopologicalMergeNetwork(task);
            FileStorageUtils.saveArtifactNetwork(
                    ArtifactType.TOPOLOGICAL_MERGE_FILE,
                    mergedNetwork,
                    task,
                    UCTE_FORMAT,
                    configuration
            );
        } catch (final Exception e) {
            final String errorMessage = String.format("Topological merge failed for task %d with target date %s, cause: %s", task.getId(), task.getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private Network getTopologicalMergeNetwork(final MergingTask task) {
        final Properties properties = new Properties();
        properties.setProperty(UCTE_IMPORT_CREATE_AREAS_KEY, "false");
        final List<Network> networks = getTopologicalMergeFiles(task).stream()
                .map(path -> readNetwork(path, properties))
                .toList();
        final String mergedNetworkName = ArtifactType.TOPOLOGICAL_MERGE_FILE.getFileName(task.getTargetDate());
        LOGGER.info("Merging {} networks into '{}'", networks.size(), mergedNetworkName);
        return Network.merge(mergedNetworkName, networks.toArray(Network[]::new));
    }

    private static Network readNetwork(final Path path, final Properties properties) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            LOGGER.info("IIDM import of network: {}", path.getFileName());
            return Network.read(path.getFileName().toString(), inputStream, LocalComputationManager.getDefault(), ImportConfig.CACHE.get(), properties);
        } catch (final Exception e) {
            throw new CeMergingException(String.format("Cannot read network '%s'", path), e);
        }
    }

    private static List<Path> getTopologicalMergeFiles(final MergingTask task) {
        final List<Path> networkFiles = new ArrayList<>();

        // Regular IGMs which have not been pre-treated
        task.getInputs().getIgms().stream()
                .filter(igm -> isNotPreTreated(task, igm))
                .map(igm -> Paths.get(igm.getIgmFile().getPath()))
                .forEach(networkFiles::add);

        // German pre-merged IGM
        networkFiles.add(Paths.get(task.getArtifactPath(GERMAN_PRE_MERGED_IGM)));

        // Dk converted IGM
        networkFiles.add(Paths.get(task.getArtifactPath(DK_CONVERTED_FILE)));

        // Pre-treated IGMs
        task.getArtifacts()
                .getPreTreatedIgmMap()
                .values()
                .stream()
                .map(SavedFile::getPath)
                .map(Paths::get)
                .forEach(networkFiles::add);

        return networkFiles;
    }

    private static boolean isNotPreTreated(final MergingTask task, final IgmData igmData) {
        return !GERMAN_AND_DANISH_TSO.contains(igmData.getCountry())
                && !task.hasPreTreatedIgm(igmData.getCountry());
    }
}

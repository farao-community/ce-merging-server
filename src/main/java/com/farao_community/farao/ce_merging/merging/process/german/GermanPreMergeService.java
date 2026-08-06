/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.ce_merging.merging.process.german;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.MergingTaskRepository;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.enums.GermanTso;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.saveArtifactNetwork;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.GERMAN_PRE_MERGED_IGM;
import static com.powsybl.computation.local.LocalComputationManager.getDefault;
import static com.powsybl.iidm.network.ImportConfig.CACHE;

@Service
public class GermanPreMergeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GermanPreMergeService.class);
    private static final Properties PARAMETERS = new Properties();

    static {
        PARAMETERS.put("ucte.import.create-areas", "false");
    }

    private final GermanMismatchCompensation germanMismatchCompensation;
    private final MergingTaskRepository tasksRepository;
    private final CeMergingConfiguration configuration;

    public GermanPreMergeService(final GermanMismatchCompensation germanMismatchCompensation,
                                 final MergingTaskRepository tasksRepository,
                                 final CeMergingConfiguration configuration) {
        this.germanMismatchCompensation = germanMismatchCompensation;
        this.tasksRepository = tasksRepository;
        this.configuration = configuration;
    }

    public void preMergeGermanCountries(final MergingTask task) {
        try {
            final Network mergedNetwork = mergeGermanRegions(task);
            LOGGER.info("German network files merged with success");
            // Save the german merged network file for investigation in case of exception in mismatch step
            saveArtifactNetwork(GERMAN_PRE_MERGED_IGM, mergedNetwork, task, UCTE_FORMAT, configuration);
            germanMismatchCompensation.apply(task, mergedNetwork);
            LOGGER.info("Replacement of German TSO's network element names by common German identifiers for network: {}", mergedNetwork);
            GermanXnodesReplacer.replaceWithLines(mergedNetwork);
            saveArtifactNetwork(GERMAN_PRE_MERGED_IGM, mergedNetwork, task, UCTE_FORMAT, configuration);
            tasksRepository.save(task);
        } catch (Exception e) {
            String errorMessage = String.format("German pre-merge failed for task %d with target date %s, cause: %s",
                                                task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    public Network mergeGermanRegions(final MergingTask task) {
        final Network[] networksToMerge = Arrays.stream(GermanTso.values())
                .map(tso -> readGermanNetwork(tso, task))
                .toArray(Network[]::new);

        return Network.merge(GERMAN_PRE_MERGED_IGM.getFileName(task.getTargetDate()), networksToMerge);
    }

    private Network readGermanNetwork(final GermanTso tso,
                                      final MergingTask task) {
        return Network.read(Path.of(task.getInputs().getIgm(tso.name()).getIgmFile().getPath()), getDefault(), CACHE.get(), PARAMETERS);
    }

}

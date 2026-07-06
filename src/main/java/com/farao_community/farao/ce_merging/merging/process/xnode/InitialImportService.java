/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.xnode;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class InitialImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialImportService.class);

    public Map<String, Network> importInitialIgms(final MergingTask taskEntity) {

        final Map<String, Network> networkByTsoMap = new HashMap<>();

        taskEntity.getInputs().getIgms().forEach(igmData -> importIgm(networkByTsoMap, igmData));
        return networkByTsoMap;
    }

    private void importIgm(final Map<String, Network> networkByTsoMap, final IgmData igmData) {
        final SavedFile igmFile = igmData.getIgmFile();
        try (FileInputStream inputStream = new FileInputStream(igmFile.getPath())) {
            LOGGER.info("Importing network file: {}", igmFile.getOriginalName());
            final Network network = Network.read(igmFile.getOriginalName(), inputStream);
            networkByTsoMap.put(igmData.getCountry(), network);

        } catch (Exception e) {
            final String errorMessage = "Network file: " + igmFile.getOriginalName() + " cannot be imported, cause: " + e.getMessage();
            LOGGER.error(errorMessage, e);
            throw new CeMergingException(errorMessage, e);
        }
    }
}

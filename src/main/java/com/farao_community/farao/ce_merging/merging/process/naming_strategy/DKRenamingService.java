/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_COUNTRY_CODE;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_HVDC_XNODES_PROPERTY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DK_NAMING_STRATEGY;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.UCTE_FORMAT;
import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.DK_CONVERTED_FILE;

@Service
public class DKRenamingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DKRenamingService.class);
    private static final String UCTE_EXPORT_NAMING_STRATEGY_PROPERTY = "ucte.export.naming-strategy";

    private final CeMergingConfiguration ceMergingConfiguration;

    public DKRenamingService(CeMergingConfiguration ceMergingConfiguration) {
        this.ceMergingConfiguration = ceMergingConfiguration;
    }

    public void renameDkCountry(MergingTask task) {
        SavedFile d1File = task.getInputs().getIgm(DK_COUNTRY_CODE).getIgmFile();
        try (InputStream inputStream = new FileInputStream(d1File.getPath())) {
            String dkHvdcXnodes = Optional.ofNullable(task.getConfigurations().getDkHvdcXnodes())
                    .orElseGet(Collections::emptyList)
                    .stream()
                    .collect(Collectors.joining(","));
            Network danishNetwork = Network.read(d1File.getOriginalName(), inputStream);
            Properties properties = buildExportProperties(dkHvdcXnodes);
            // danishNetwork.setProperty(DK_HVDC_XNODES_PROPERTY, dkHvdcXnodes); : copied from core-merging todo check if this set is mondotary or network.write(UCTE_FORMAT, properties, filePath);
            FileStorageUtils.saveArtifactNetwork(
                DK_CONVERTED_FILE, danishNetwork, task, UCTE_FORMAT, properties, ceMergingConfiguration
            );
        } catch (Exception e) {
            String errorMessage = String.format("Denmark Renaming strategy failed for task %d with target date %s, cause: %s", task.getId(), task.getInputs().getTargetDate(), e.getMessage());
            LOGGER.error(errorMessage);
            throw new CeMergingException(errorMessage, e);
        }
    }

    private Properties buildExportProperties(String dkHvdcXnodes) {
        Properties properties = new Properties();
        properties.setProperty(UCTE_EXPORT_NAMING_STRATEGY_PROPERTY, DK_NAMING_STRATEGY);
        properties.setProperty(DK_HVDC_XNODES_PROPERTY, dkHvdcXnodes);
        return properties;
    }
}


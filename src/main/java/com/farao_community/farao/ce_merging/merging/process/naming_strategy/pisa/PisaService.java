/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process.naming_strategy.pisa;

import com.farao_community.farao.ce_merging.common.config.CeMergingConfiguration;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType;
import com.powsybl.iidm.network.Network;
import org.springframework.stereotype.Service;

import java.util.Properties;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.*;
import static com.farao_community.farao.ce_merging.common.util.FileStorageUtils.save;

@Service
public class PisaService {
    private final CeMergingConfiguration configuration;

    public PisaService(final CeMergingConfiguration configuration) {
        this.configuration = configuration;
    }

    public void renameNode(final MergingTask task) {
        final Properties properties = new Properties();
        properties.setProperty(UCTE_EXPORT_NAMING_STRATEGY_PROPERTY, PISA_NAMING_STRATEGY);

        final SavedFile cgmFile = save(configuration.getOutputsDirectoryPath(task),
                task.getOutputCgmFileName(),
                String.format("/tasks/%d/outputs/cgm", task.getId()),
                path -> getCgm(task).write(UCTE_FORMAT, properties, path));

        task.getOutputs().setCgm(cgmFile);
    }

    private static Network getCgm(final MergingTask task) {
        return Network.read(task.getArtifactPath(ArtifactType.CGM_FILE_AFTER_COMPENSATION));
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.ArtifactsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.farao_community.farao.ce_merging.merging.task.enums.ArtifactType.CGM_NET_POSITIONS_FILE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MergingTaskMapperTest {

    @Autowired
    MergingTaskMapper mapper;

    @Test
    void shouldMapTaskWithArtifacts() {
        final MergingTask task = new MergingTask();
        final Artifacts artifacts = new Artifacts();
        final SavedFile file = new SavedFile();
        file.setLocation(".");
        artifacts.putFile(CGM_NET_POSITIONS_FILE, file);
        task.setArtifacts(artifacts);
        task.setInputs(new Inputs());

        final ArtifactsDto dto = mapper.mergingTaskToMergingTaskDto(task).getArtifacts();

        assertThat(dto.getCgmNetPositionsFileLocation()).isNotNull();
        assertThat(dto.getGlskQualityReportLocation()).isNull();
    }

}

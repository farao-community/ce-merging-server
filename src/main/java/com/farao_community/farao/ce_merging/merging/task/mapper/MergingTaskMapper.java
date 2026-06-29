/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.mapper;

import com.farao_community.farao.ce_merging.merging.task.dto.ArtifactsDto;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.enums.ArtifactType;
import org.mapstruct.Mapper;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface MergingTaskMapper {

    MergingTaskDto mergingTaskToMergingTaskDto(final MergingTask mergingTask);

    default ArtifactsDto artifactsToArtifactsDto(final Artifacts artifacts) {
        final ArtifactsDto mapped = new ArtifactsDto();

        for (final ArtifactType type : ArtifactType.values()) {
            Optional.ofNullable(artifacts.getFile(type))
                .map(SavedFile::getLocation)
                .ifPresent(loc -> mapped.putLocation(type, loc));
        }

        return mapped;
    }
}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.dto;

import com.farao_community.farao.ce_merging.merging.enums.ArtifactType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.ElementCollection;

import java.io.Serializable;
import java.util.EnumMap;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public class ArtifactsDto implements Serializable {

    @ElementCollection
    private final EnumMap<ArtifactType, String> artifactsLocations = new EnumMap<>(ArtifactType.class);

    public String getLocation(final ArtifactType type) {
        return artifactsLocations.get(type);
    }

    public void putLocation(final ArtifactType type, final String location) {
        artifactsLocations.put(type, location);
    }

}

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import com.farao_community.farao.ce_merging.merging.task.dto.ArtifactsDto;
import com.farao_community.farao.ce_merging.merging.task.dto.ConfigurationsDto;
import com.farao_community.farao.ce_merging.merging.task.dto.IgmDto;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.dto.OutputsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Xnode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import test_utils.GetterSetterVerifier;

import java.util.List;

class EntitiesAndDtosTest {
    static List<Class<?>> dtos = List.of(ArtifactsDto.class,
                                         ConfigurationsDto.class,
                                         IgmDto.class,
                                         OutputsDto.class,
                                         MergingTaskDto.class);

    static List<Class<?>> entities = List.of(Artifacts.class,
                                             BorderDirectionRecord.class,
                                             Configurations.class,
                                             IgmData.class,
                                             MergingTask.class,
                                             Outputs.class,
                                             SavedFile.class,
                                             VirtualHubRecord.class,
                                             Xnode.class);

    @ParameterizedTest
    @FieldSource("entities")
    void entitiesShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

    @ParameterizedTest
    @FieldSource("dtos")
    void dtosShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz).verify();
    }

}

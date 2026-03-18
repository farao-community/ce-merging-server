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
import com.farao_community.farao.ce_merging.merging.task.dto.InputsDto;
import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.merging.task.entities.Artifacts;
import com.farao_community.farao.ce_merging.merging.task.entities.BorderDirectionRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Configurations;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.MergingTask;
import com.farao_community.farao.ce_merging.merging.task.entities.Outputs;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import com.farao_community.farao.ce_merging.merging.task.entities.VirtualHubRecord;
import com.farao_community.farao.ce_merging.merging.task.entities.Xnode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import test_utils.GetterSetterVerifier;

import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntitiesAndDtosTest {
    static List<Class<?>> dtos = List.of(ArtifactsDto.class,
                                         ConfigurationsDto.class,
                                         IgmDto.class,
                                         InputsDto.class,
                                         MergingTaskDto.class);

    static List<Class<?>> entities = List.of(Artifacts.class,
                                             BorderDirectionRecord.class,
                                             Configurations.class,
                                             IgmData.class,
                                             Inputs.class,
                                             MergingTask.class,
                                             Outputs.class,
                                             SavedFile.class,
                                             VirtualHubRecord.class,
                                             Xnode.class);

    @ParameterizedTest
    @FieldSource("entities")
    void entitiesShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz)
            .exclude("realOffset")
            .exclude("alegroThreshold")
            .verify();
    }

    @ParameterizedTest
    @FieldSource("dtos")
    void dtosShouldHaveStandardGettersAndSetters(final Class<?> clazz) {
        GetterSetterVerifier.forClass(clazz)
            .exclude("realOffset")
            .exclude("alegroThreshold")
            .verify();
    }

    @Test
    void shouldHaveDefaultValuesForSomeGetters() {
        final Inputs inputs = new Inputs();
        final InputsDto inputsDto = new InputsDto();

        assertEquals(2000, inputs.getAlegroThreshold());
        assertEquals(2000, inputsDto.getAlegroThreshold());
        assertEquals(ZoneOffset.of("+01:00"), inputs.getRealOffset());

        // but the setters still work as intended
        inputs.setAlegroThreshold(1234);
        assertEquals(1234, inputs.getAlegroThreshold());

        inputsDto.setAlegroThreshold(1234);
        assertEquals(1234, inputsDto.getAlegroThreshold());

        inputs.setRealOffset(ZoneOffset.UTC);
        assertEquals(ZoneOffset.UTC, inputs.getRealOffset());
    }
}

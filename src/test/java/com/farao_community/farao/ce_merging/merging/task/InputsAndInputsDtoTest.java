/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task;

import com.farao_community.farao.ce_merging.merging.task.dto.InputsDto;
import com.farao_community.farao.ce_merging.merging.task.entities.IgmData;
import com.farao_community.farao.ce_merging.merging.task.entities.Inputs;
import com.farao_community.farao.ce_merging.merging.task.entities.enums.IgmType;
import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.time.ZoneOffset;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputsAndInputsDtoTest {

    @Test
    void shouldHaveStandardGettersAndSetters() {
        GetterSetterVerifier.forClass(Inputs.class)
            .exclude("realOffset")
            .exclude("alegroThreshold")
            .exclude("mergingWithInternalHvdc")
            .verify();

        GetterSetterVerifier.forClass(InputsDto.class)
            .exclude("realOffset")
            .exclude("alegroThreshold")
            .exclude("mergingWithInternalHvdc")
            .verify();
    }

    @Test
    void shouldHaveDefaultValuesForSomeGetters() {
        final Inputs inputs = new Inputs();
        final InputsDto inputsDto = new InputsDto();

        assertEquals(2000, inputs.getAlegroThreshold());
        assertEquals(2000, inputsDto.getAlegroThreshold());
        assertEquals(ZoneOffset.of("+01:00"), inputs.getRealOffset());
        assertTrue(inputs.isMergingWithInternalHvdc());

        // but the setters still work as intended
        inputs.setAlegroThreshold(1234);
        assertEquals(1234, inputs.getAlegroThreshold());

        inputsDto.setAlegroThreshold(1234);
        assertEquals(1234, inputsDto.getAlegroThreshold());

        inputs.setRealOffset(UTC);
        assertEquals(UTC, inputs.getRealOffset());

        inputs.setMergingWithInternalHvdc(false);
        assertFalse(inputs.isMergingWithInternalHvdc());
    }

    @Test
    void shouldGetOneIgmInInputs() {
        final IgmData france = new IgmData();
        france.setCountry("FR");
        france.setType(IgmType.SNAPSHOT);
        final IgmData spain = new IgmData();
        spain.setCountry("ES");
        spain.setType(IgmType.SNAPSHOT);

        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(france, spain));

        assertEquals(inputs.getIgm("FR"), france);
    }

}

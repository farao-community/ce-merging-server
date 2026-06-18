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
import org.junit.jupiter.api.Test;
import test_utils.GetterSetterVerifier;

import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DEFAULT_ALEGRO_THRESHOLD;
import static com.farao_community.farao.ce_merging.common.CeMergingConstants.DEFAULT_REQUEST_OFFSET;
import static com.farao_community.farao.ce_merging.merging.task.enums.IgmType.SNAPSHOT;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputsAndInputsDtoTest {

    private static final int NEW_ALEGRO_THRESHOLD = 1234;



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

        assertEquals(DEFAULT_ALEGRO_THRESHOLD, inputs.getAlegroThreshold());
        assertEquals(DEFAULT_ALEGRO_THRESHOLD, inputsDto.getAlegroThreshold());
        assertEquals(DEFAULT_REQUEST_OFFSET, inputs.getRealOffset());
        assertTrue(inputs.getMergingWithInternalHvdc());
        assertTrue(inputs.getMergingWithInternalHvdc());
    }

    @Test
    void shouldHaveSettersForDefaultingGetters() {
        final Inputs inputs = new Inputs();
        final InputsDto inputsDto = new InputsDto();

        inputs.setAlegroThreshold(NEW_ALEGRO_THRESHOLD);
        assertEquals(NEW_ALEGRO_THRESHOLD, inputs.getAlegroThreshold());

        inputsDto.setAlegroThreshold(NEW_ALEGRO_THRESHOLD);
        assertEquals(NEW_ALEGRO_THRESHOLD, inputsDto.getAlegroThreshold());

        inputs.setRealOffset(UTC);
        assertEquals(UTC, inputs.getRealOffset());

        inputs.setMergingWithInternalHvdc(false);
        assertFalse(inputs.getMergingWithInternalHvdc());

        inputsDto.setMergingWithInternalHvdc(false);
        assertFalse(inputsDto.getMergingWithInternalHvdc());
    }

    @Test
    void shouldGetOneIgmInInputs() {
        final IgmData france = new IgmData();
        france.setCountry("FR");
        france.setType(SNAPSHOT);
        final IgmData spain = new IgmData();
        spain.setCountry("ES");
        spain.setType(SNAPSHOT);

        final Inputs inputs = new Inputs();
        inputs.setIgms(List.of(france, spain));

        assertEquals(inputs.getIgm("FR"), france);
    }

}

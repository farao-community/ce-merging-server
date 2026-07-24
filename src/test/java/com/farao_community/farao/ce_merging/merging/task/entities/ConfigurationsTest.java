/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.task.entities;

import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.merging.process.recessivity.RecessivityParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class ConfigurationsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnCustomListWhenPathIsValid() throws IOException {
        Path customFile = tempDir.resolve("custom-recessivity.json");
        List<String> customCountries = List.of("FR", "BE");
        RecessivityParameters params = new RecessivityParameters(customCountries);
        JsonUtils.writeInPath(RecessivityParameters.class, params, customFile);

        Configurations configurations = new Configurations();
        configurations.setRecessivityParametersFilePath(customFile.toString());

        List<String> result = configurations.getOrDefaultRecessiveCountries();

        assertEquals(customCountries, result);
    }

    @Test
    void shouldReturnDefaultListWhenPathIsInvalid() {
        Configurations configurations = new Configurations();
        configurations.setRecessivityParametersFilePath("non-existing-file.json");

        List<String> result = configurations.getOrDefaultRecessiveCountries();

        assertEquals(14, result.size());
        assertTrue(result.contains("AL"));
        assertTrue(result.contains("UA"));
    }

    @Test
    void shouldReturnEmptyListWhenBothPathsFail() {
        try (MockedConstruction<ClassPathResource> mockedClassPathResource = mockConstruction(ClassPathResource.class, (mock, context) -> {
            when(mock.getInputStream()).thenThrow(new IOException("Default file not found"));
        })) {
            Configurations configurations = new Configurations();
            configurations.setRecessivityParametersFilePath("non-existing-file.json");

            List<String> result = configurations.getOrDefaultRecessiveCountries();

            assertTrue(result.isEmpty());
        }
    }
}

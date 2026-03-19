/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OffsetDateTimeSerializerTest {

    @Test
    void shouldSerialize() throws IOException {
        final OffsetDateTime timestamp = OffsetDateTime.parse("2025-12-08T14:00Z");

        final Writer jsonWriter = new StringWriter();
        final JsonGenerator jsonGenerator = new JsonFactory().createGenerator(jsonWriter);
        final SerializerProvider serializerProvider = new ObjectMapper().getSerializerProvider();

        new OffsetDateTimeSerializer().serialize(timestamp, jsonGenerator, serializerProvider);
        jsonGenerator.flush();

        assertThat(jsonWriter)
            .hasToString("\"2025-12-08T14:00Z\"");
    }

}

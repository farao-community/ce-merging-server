/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.farao_community.farao.ce_merging.common.CeMergingTestUtils.stringPathOfTestFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilsTest {

    @Data
    @EqualsAndHashCode
    static class DummyJson {
        String stringValue;
        Boolean boolValue;
        Integer numValue;
        List<String> listValue;

        public DummyJson(@JsonProperty("stringValue") final String stringValue,
                         @JsonProperty("boolValue") final Boolean boolValue,
                         @JsonProperty("numValue") final Integer numValue,
                         @JsonProperty("listValue") final List<String> listValue) {
            this.stringValue = stringValue;
            this.boolValue = boolValue;
            this.numValue = numValue;
            this.listValue = listValue;
        }
    }

    private static final byte[] JSON_CONTENT = """
        {
          "stringValue" : "test_as_bytes",
          "boolValue" : true,
          "numValue" : 0,
          "listValue" : [ "1", "2", "3" ]
        }""".getBytes(UTF_8);

    private static final DummyJson TEST_JSON = new DummyJson("test",
                                                             true,
                                                             0,
                                                             List.of("1", "2", "3"));

    private static final DummyJson TEST_JSON_FROM_BYTES = new DummyJson("test_as_bytes",
                                                                        true,
                                                                        0,
                                                                        List.of("1", "2", "3"));

    @Test
    void shouldReadObjectFromJson() throws IOException {
        assertEquals(TEST_JSON,
                     JsonUtils.read(DummyJson.class, stringPathOfTestFile("dummy.json")));
        final MockMultipartFile multipartFile = new MockMultipartFile("dummy.json", JSON_CONTENT);
        assertEquals(TEST_JSON_FROM_BYTES,
                     JsonUtils.read(DummyJson.class, multipartFile));
    }

    @Test
    void shouldWriteObjectToJson() throws IOException {
        assertArrayEquals(JSON_CONTENT,
                          JsonUtils.writeToBytes(DummyJson.class, TEST_JSON_FROM_BYTES));

        final Path newFile = Files.createFile(Files.createTempDirectory("json-test")
                                                  .resolve("dummy.json"));

        JsonUtils.writeInPath(DummyJson.class, TEST_JSON, newFile);

        assertEquals(TEST_JSON,
                     JsonUtils.read(DummyJson.class, newFile.toString()));
    }
}

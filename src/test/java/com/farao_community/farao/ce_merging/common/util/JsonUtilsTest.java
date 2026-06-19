/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.getFailingInputStream;
import static test_utils.CeTestUtils.getFailingOutputStream;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class JsonUtilsTest {

    private static final byte[] JSON_CONTENT = """
        {
          "stringValue" : "test_as_bytes",
          "boolValue" : true,
          "numValue" : 0,
          "listValue" : [ "1", "2", "3" ]
        }""".getBytes(UTF_8);

    private static final Class<DummyJson> DUMMY_CLASS = DummyJson.class;

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
                     JsonUtils.read(DUMMY_CLASS, stringPathOf("dummy.json")));
        final MockMultipartFile multipartFile = new MockMultipartFile("dummy.json", JSON_CONTENT);
        assertEquals(TEST_JSON_FROM_BYTES,
                     JsonUtils.read(DUMMY_CLASS, multipartFile));
    }

    @Test
    void shouldWriteObjectToJson() throws IOException {
        assertArrayEquals(JSON_CONTENT,
                          JsonUtils.writeToBytes(DUMMY_CLASS, TEST_JSON_FROM_BYTES));

        final Path newFile = Files.createFile(Files.createTempDirectory("json-test")
                                                  .resolve("dummy.json"));

        JsonUtils.writeInPath(DUMMY_CLASS, TEST_JSON, newFile);

        assertEquals(TEST_JSON,
                     JsonUtils.read(DUMMY_CLASS, newFile.toString()));
    }

    @Test
    void shouldFailWithInvalidStreams() {
        assertThatThrownBy(() -> JsonUtils.read(DUMMY_CLASS, getFailingInputStream()))
            .isValidServiceException()
            .hasMessage("Error occurred when converting JSON file to object of type DummyJson");

        assertThatThrownBy(() -> JsonUtils.writeInStream(DUMMY_CLASS, TEST_JSON, getFailingOutputStream()))
            .isValidServiceException()
            .hasMessage("Error occurred when writing content of object of type DummyJson");
    }

    private static class DummyJson {
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

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof final DummyJson dummyJson)) {
                return false;
            }
            return Objects.equals(stringValue, dummyJson.getStringValue()) && Objects.equals(boolValue, dummyJson.getBoolValue()) && Objects.equals(numValue, dummyJson.getNumValue()) && Objects.equals(listValue, dummyJson.getListValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(stringValue, boolValue, numValue, listValue);
        }

        public Integer getNumValue() {
            return numValue;
        }

        public void setNumValue(final Integer numValue) {
            this.numValue = numValue;
        }

        public String getStringValue() {
            return stringValue;
        }

        public void setStringValue(final String stringValue) {
            this.stringValue = stringValue;
        }

        public Boolean getBoolValue() {
            return boolValue;
        }

        public void setBoolValue(final Boolean boolValue) {
            this.boolValue = boolValue;
        }

        public List<String> getListValue() {
            return listValue;
        }

        public void setListValue(final List<String> listValue) {
            this.listValue = listValue;
        }
    }
}

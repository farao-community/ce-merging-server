/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.merging.task.dto.MergingTaskDto;
import com.farao_community.farao.ce_merging.xsd.Xnode;
import com.farao_community.farao.ce_merging.xsd.Xnodes;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static test_utils.CeTestUtils.stringPathOf;
import static test_utils.CeTestUtils.throwers;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class JaxbUtilsTest {

    private static final byte[] XML_CONTENT = """
        <?xml version="1.0"?>
        <xnodes xmlns="http://www.rte-france.com/gsr" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <xnode name="TEST_NODE_AS_BYTES" area1="FR" area2="DE" subarea2="D2"/>
        </xnodes>
        """.getBytes(UTF_8);

    private static final Class<Xnodes> XNODES_CLASS = Xnodes.class;
    private static final Class<MergingTaskDto> TASK_CLASS = MergingTaskDto.class;
    private static final MergingTaskDto NEW_TASK = new MergingTaskDto();

    @Test
    void shouldUnmarshallDummyXml() {
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES_CLASS, stringPathOf("testXnode.xml"))
                         .getXnode()
                         .getFirst()
                         .getName());

        assertEquals("TEST_NODE_AS_BYTES",
                     JaxbUtils.readFromBytes(XNODES_CLASS, XML_CONTENT)
                         .getXnode()
                         .getFirst()
                         .getName());
    }

    @Test
    void shouldMarshall() throws IOException {
        final Xnodes xnodes = new Xnodes();
        final Xnode xnode = new Xnode();
        xnode.setName("TEST_NODE");
        xnode.setArea1("FR");
        xnode.setArea2("DE");
        xnode.setSubarea2("D2");
        xnodes.getXnode().add(xnode);

        /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                      BYTES
         -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/
        assertThat(new String(JaxbUtils.writeToBytes(XNODES_CLASS, xnodes)))
            .contains("name=\"TEST_NODE\"",
                      "area1=\"FR\"",
                      "area2=\"DE\"",
                      "subarea2=\"D2\"");

        //specifying root
        assertThat(new String(JaxbUtils.writeToBytes(XNODES_CLASS,
                                                     xnodes,
                                                     "http://www.rte-france.com/gsr",
                                                     "xnodes")))
            .contains("name=\"TEST_NODE\"",
                      "area1=\"FR\"",
                      "area2=\"DE\"",
                      "subarea2=\"D2\"");

        /*+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
                      PATH
         -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-*/

        final Path newFile = Files.createFile(Files.createTempDirectory("jaxb-test")
                                                  .resolve("xnodes.xml"));

        JaxbUtils.writeToPath(XNODES_CLASS, xnodes, newFile);
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES_CLASS, newFile.toString())
                         .getXnode()
                         .getFirst()
                         .getName());

        //specifying root
        JaxbUtils.writeToPath(XNODES_CLASS, xnodes, "http://www.rte-france.com/gsr",
                              "xnodes", newFile);
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES_CLASS, newFile.toString())
                         .getXnode()
                         .getFirst()
                         .getName());

    }

    @ParameterizedTest
    @ValueSource(strings = {"dummy.csv", "dummy.json"})
    void shouldFailOnReadInvalidData(final String fileName) {
        final String strInput = stringPathOf(fileName);
        final byte[] byteInput = fileName.getBytes(UTF_8);

        assertThatThrownBy(() -> JaxbUtils.readFromPath(XNODES_CLASS, strInput))
            .isServiceException()
            .hasMessageContaining("Xnode");

        assertThatThrownBy(() -> JaxbUtils.readFromBytes(XNODES_CLASS, byteInput))
            .isServiceException()
            .hasMessageContaining("Xnode");
    }

    @Test
    void shouldFailOnReadMissingFile() {
        assertThatThrownBy(() -> JaxbUtils.readFromPath(XNODES_CLASS, "/non/existent/path.xml"))
            .isServiceException()
            .hasMessageContaining("Xnode");

        assertThatThrownBy(() -> JaxbUtils.readFromPath(XNODES_CLASS, null))
            .isServiceException()
            .hasMessageContaining("Xnode");
    }

    static Stream<ThrowableAssert.ThrowingCallable> failingWriters() {
        return throwers(() -> JaxbUtils.writeToBytes(TASK_CLASS, NEW_TASK),
                        () -> JaxbUtils.writeToPath(TASK_CLASS, NEW_TASK, Path.of("/nothing")),
                        () -> JaxbUtils.writeToPath(TASK_CLASS, NEW_TASK, null),
                        () -> JaxbUtils.writeToPath(TASK_CLASS, NEW_TASK, null, null, Path.of("/nothing")),
                        () -> JaxbUtils.writeToBytes(TASK_CLASS, NEW_TASK, null, null));
    }

    @ParameterizedTest
    @MethodSource("failingWriters")
    void shouldFailOnWriteInvalidClass(final ThrowableAssert.ThrowingCallable thrower) {
        assertThatThrownBy(thrower)
            .isServiceException()
            .hasMessageContaining("MergingTaskDto");

    }

}

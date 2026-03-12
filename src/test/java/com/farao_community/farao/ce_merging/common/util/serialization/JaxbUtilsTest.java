/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.xsd.Xnode;
import com.farao_community.farao.ce_merging.xsd.Xnodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.farao_community.farao.ce_merging.common.CeMergingTestUtils.stringPathOfTestFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JaxbUtilsTest {

    private static final byte[] XML_CONTENT = """
        <?xml version="1.0"?>
        <xnodes xmlns="http://www.rte-france.com/gsr" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <xnode name="TEST_NODE_AS_BYTES" area1="FR" area2="DE" subarea2="D2"/>
        </xnodes>
        """.getBytes(UTF_8);

    private static final Class<Xnodes> XNODES = Xnodes.class;

    @Test
    void shouldUnmarshallDummyXml() {
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES, stringPathOfTestFile("testXnode.xml"))
                         .getXnode()
                         .getFirst()
                         .getName());

        assertEquals("TEST_NODE_AS_BYTES",
                     JaxbUtils.readFromBytes(XNODES, XML_CONTENT)
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
        assertThat(new String(JaxbUtils.writeToBytes(XNODES, xnodes)))
            .contains("name=\"TEST_NODE\"",
                      "area1=\"FR\"",
                      "area2=\"DE\"",
                      "subarea2=\"D2\"");

        //specifying root
        assertThat(new String(JaxbUtils.writeToBytes(XNODES,
                                                     xnodes,
                                                     "\"http://www.rte-france.com/gsr\"",
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

        JaxbUtils.writeToPath(XNODES, xnodes, newFile);
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES, newFile.toString())
                         .getXnode()
                         .getFirst()
                         .getName());

        //specifying root
        JaxbUtils.writeToPath(XNODES, xnodes, "\"http://www.rte-france.com/gsr\"",
                              "xnodes", newFile);
        assertEquals("TEST_NODE",
                     JaxbUtils.readFromPath(XNODES, newFile.toString())
                         .getXnode()
                         .getFirst()
                         .getName());

    }

    @ParameterizedTest
    @ValueSource(strings = {"dummy.csv", "dummy.json"})
    void shouldFailOnIncorrectData(final String fileName) {
        final String strInput = stringPathOfTestFile(fileName);
        final byte[] byteInput = fileName.getBytes(UTF_8);
        assertThrows(ServiceIOException.class,
                     () -> JaxbUtils.readFromPath(XNODES, strInput));
        assertThrows(ServiceIOException.class,
                     () -> JaxbUtils.readFromBytes(XNODES, byteInput));
    }

}

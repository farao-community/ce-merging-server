/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import static com.farao_community.farao.ce_merging.common.util.ZipUtils.addFileToZip;
import static com.farao_community.farao.ce_merging.common.util.ZipUtils.unzipFile;
import static com.farao_community.farao.ce_merging.common.util.ZipUtils.zipDirectory;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.pathOf;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class ZipUtilsTest {
    private static final String TEST_ZIP = "testZip.zip";

    @TempDir
    Path tempDir;

    @Test
    void shouldUnzipThenZipAgain() throws IOException {
        final Path parent = Files.createTempDirectory("zip-test");
        unzipFile(pathOf(TEST_ZIP), parent);

        Stream.of("file1.txt",
                  "file2.txt",
                  "directory/file3.txt",
                  "existing-dir/file4.txt")
            .map(parent::resolve)
            .forEach(path -> assertTrue(Files.exists(path)));

        assertTrue(zipDirectory(parent.toString()).length > 0);
    }

    @Test
    void shouldFailWhenUnzippingToReadOnlyDirectory() {
        final Path tmp = tempDir;
        assertTrue(tmp.toFile().setReadOnly());
        final Path zipInput = pathOf(TEST_ZIP);
        assertThatThrownBy(() -> unzipFile(zipInput, tmp))
            .isServiceException()
            .hasMessageContaining(TEST_ZIP);
    }

    @Test
    void shouldFailWhenUnzippingToInvalidDirectory() {
        assertThatThrownBy(() -> unzipFile(pathOf(TEST_ZIP), Path.of("/not/existing")))
            .isServiceException()
            .hasMessage("Error occurred while extracting file testZip.zip");
    }

    @Test
    void shouldFailWhenZippingToInvalidPath() {
        assertThatThrownBy(() -> zipDirectory("not/existing"))
            .isServiceException()
            .hasMessageContaining("Error occurred while compressing directory not/existing");

        assertThatThrownBy(() -> zipDirectory("@%$*!:;"))
            .hasMessageContaining("Error occurred while compressing directory @%$*!:;");

        assertThatThrownBy(() -> addFileToZip(Path.of("/not/existing"), "/nowhere",
                                              new ZipOutputStream(new ByteArrayOutputStream(0))))
            .hasMessageContaining("Error during output ZIP creation");

    }

}

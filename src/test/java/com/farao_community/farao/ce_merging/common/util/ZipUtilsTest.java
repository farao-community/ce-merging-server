/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.farao_community.farao.ce_merging.common.util.ZipUtils.unzipFile;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static test_utils.CeTestUtils.pathOf;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class ZipUtilsTest {
    @Test
    void shouldUnzipThenZipAgain() throws IOException {
        final Path tmp = Files.createTempDirectory("zip-test");
        unzipFile(pathOf("testZip.zip"), tmp);

        assertTrue(pathInZip("file1.txt", tmp)
                       .toFile().exists());
        assertTrue(pathInZip("file2.txt", tmp)
                       .toFile().exists());
        assertTrue(pathInZip("directory/file3.txt", tmp)
                       .toFile().exists());

        assertTrue(ZipUtils.zipDirectory(tmp.toString()).length > 0);
    }

    static Path pathInZip(final String fileName, final Path tmp) {
        return Paths.get(tmp.toString(), fileName);
    }

    @Test
    void shouldFailWhenUnzippingToReadOnlyDirectory() throws IOException {
        final Path tmp = Files.createTempDirectory("zip-test");
        assertTrue(tmp.toFile().setReadOnly());
        final Path zipInput = pathOf("testZip.zip");
        assertThatThrownBy(() -> unzipFile(zipInput, tmp))
            .isServiceException();
    }
}

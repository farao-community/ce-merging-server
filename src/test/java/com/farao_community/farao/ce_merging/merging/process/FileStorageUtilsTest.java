/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.FileStorageUtils;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FileStorageUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveFile() throws IOException {
        final String fileName = "test.txt";
        final String location = "/tasks/1/artifacts/test";
        final SavedFile savedFile = FileStorageUtils.save(
            tempDir.toString(),
            fileName,
            location,
            path -> {
                try {
                    Files.writeString(path, "content");
                } catch (IOException e) {
                    throw new CeMergingException("Cannot write test file", e);
                }
            }
        );
        final Path expectedPath = tempDir.resolve(fileName);

        assertTrue(Files.exists(expectedPath));
        assertEquals("content", Files.readString(expectedPath));
        assertEquals(fileName, savedFile.getOriginalName());
        assertEquals(expectedPath.toString(), savedFile.getPath());
        assertEquals(location, savedFile.getLocation());
    }

    @Test
    void shouldThrowCeMergingExceptionWhenWriterFails() {
        final CeMergingException exception = assertThrows(
            CeMergingException.class,
            () -> FileStorageUtils.save(
                tempDir.toString(),
                "test.txt",
                "/location",
                path -> {
                    throw new IllegalStateException("exception");
                })
        );

        assertEquals("Cannot write file 'test.txt'", exception.getMessage());
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }
}

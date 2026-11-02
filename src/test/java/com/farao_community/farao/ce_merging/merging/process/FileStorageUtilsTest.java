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

class FileStorageUtilsTest {

    private static final String FILE_NAME = "test.txt";
    private static final String FILE_CONTENT = "content";
    private static final String LOCATION = "/tasks/1/artifacts/test";

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveFile() throws IOException {
        final SavedFile savedFile = FileStorageUtils.save(
            tempDir.toString(),
            FILE_NAME,
            LOCATION,
            path -> {
                try {
                    Files.writeString(path, FILE_CONTENT);
                } catch (IOException e) {
                    throw new CeMergingException("Cannot write test file", e);
                }
            }
        );
        final Path expectedPath = tempDir.resolve(FILE_NAME);

        assertTrue(Files.exists(expectedPath));
        assertEquals(FILE_CONTENT, Files.readString(expectedPath));
        assertEquals(FILE_NAME, savedFile.getOriginalName());
        assertEquals(expectedPath.toString(), savedFile.getPath());
        assertEquals(LOCATION, savedFile.getLocation());
    }

    @Test
    void shouldThrowCeMergingExceptionWhenWriterFails() {
        final IllegalStateException cause = new IllegalStateException("exception");
        final CeMergingException exception = assertThrows(
            CeMergingException.class,
            () -> FileStorageUtils.save(
                tempDir.toString(),
                FILE_NAME,
                "/location",
                path -> {
                    throw cause;
                })
        );

        assertEquals("Cannot write file 'test.txt'", exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}

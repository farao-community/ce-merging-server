/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.farao_community.farao.ce_merging.CeMergingTestUtils.stringPathOfTestFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileUtilsTest {

    @Test
    void shouldCreateAttachments() {
        final ResponseEntity<byte[]> response = FileUtils.toAttachmentFileResponse("hello".getBytes(UTF_8),
                                                                                   "hello.txt");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("hello", new String(response.getBody()));

        final SavedFile file = new SavedFile();
        file.setLocation("testFiles");
        file.setOriginalName("testXnode.xml");
        file.setPath(stringPathOfTestFile("testXnode.xml"));
        file.setFileId(1);

        final ResponseEntity<byte[]> responseXml = FileUtils.toAttachmentFileResponse(file);

        assertEquals(HttpStatus.OK, responseXml.getStatusCode());
        assertNotNull(responseXml.getBody());
    }

    @Test
    void shouldFailIfFileDoNotExists() {
        final SavedFile savedFile = new SavedFile("filename.txt",
                                                  "/path/to/non/existing/file.txt",
                                                  "/dumb/file/location");
        assertThrows(ServiceIOException.class, () -> FileUtils.toAttachmentFileResponse(savedFile));
    }

}

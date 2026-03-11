/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.merging.entities.SavedFile;
import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public class HttpFileUtils {
    private static final String ATTACHMENT_ERROR = "Cannot return attachment file";
    private static final String RETRIEVE_ERROR = "Cannot retrieve content of ";

    public static ResponseEntity<byte[]> toAttachmentFileResponse(final byte[] fileContent,
                                                          final String fileName) {
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition
                                              .builder("attachment")
                                              .filename(fileName)
                                              .build());
            return ResponseEntity.ok()
                .contentType(APPLICATION_OCTET_STREAM)
                .headers(headers)
                .body(fileContent);

        } catch (final Exception e) {
            log.error(ATTACHMENT_ERROR);
            throw new ServiceIOException(ATTACHMENT_ERROR, e);
        }
    }

    public static ResponseEntity<byte[]> toAttachmentFileResponse(final SavedFile savedFile) {
        try {
            final byte[] fileContent = Files.readAllBytes(Paths.get(savedFile.getPath()));
            final String fileName = savedFile.getOriginalName();
            return toAttachmentFileResponse(fileContent, fileName);
        } catch (final IOException | ServiceIOException e) {
            log.error(RETRIEVE_ERROR + "'{}'", savedFile.getPath());
            throw new ServiceIOException(String.format(RETRIEVE_ERROR + "%s", savedFile.getPath()), e);
        }
    }
}

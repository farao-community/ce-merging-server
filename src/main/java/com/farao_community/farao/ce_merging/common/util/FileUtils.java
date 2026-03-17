/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.Files.readAllBytes;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

public final class FileUtils {
    private static final String ATTACHMENT_ERROR = "Cannot return attachment file";
    private static final String RETRIEVE_ERROR = "Cannot retrieve content of ";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
        // utility class
    }

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
            LOGGER.error(ATTACHMENT_ERROR);
            throw new ServiceIOException(ATTACHMENT_ERROR, e);
        }
    }

    public static ResponseEntity<byte[]> toAttachmentFileResponse(final SavedFile savedFile) {
        final String path = savedFile.getPath();
        try {
            return toAttachmentFileResponse(readAllBytes(Paths.get(path)),
                                            savedFile.getOriginalName());
        } catch (final IOException | ServiceIOException e) {
            LOGGER.error(RETRIEVE_ERROR + "'{}'", path);
            throw new ServiceIOException(String.format(RETRIEVE_ERROR + "%s", path), e);
        }
    }

    public static Path getIfInside(final String pathToGet,
                                   final Path parentFolder) {
        if (isEmpty(pathToGet)) {
            throw new ServiceIOException("Missing file path");
        }
        final Path parent = parentFolder.normalize();
        final Path resolved = parent.resolve(pathToGet).normalize();
        if (!resolved.startsWith(parent)) {
            throw new ServiceIOException("Invalid file path : %s".formatted(pathToGet));
        }
        return resolved;
    }
}

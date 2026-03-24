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
    private static final String RETRIEVE_ERROR = "Cannot retrieve content of %s";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
        // utility class
    }

    /**
     *
     * @param fileContent the content of a file as a byte array
     * @return the file wrapped in an HTTP response entity
     */
    public static ResponseEntity<byte[]> toAttachmentFileResponse(final byte[] fileContent,
                                                                  final String fileName) {
        // no try/catch here because nothing can ever throw
        final HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition
                                          .builder("attachment")
                                          .filename(fileName)
                                          .build());
        return ResponseEntity.ok()
            .contentType(APPLICATION_OCTET_STREAM)
            .headers(headers)
            .body(fileContent);

    }

    /**
     *
     * @param savedFile a file of the merging process
     * @return the file wrapped in an HTTP response entity
     */
    public static ResponseEntity<byte[]> toAttachmentFileResponse(final SavedFile savedFile) {
        final String path = savedFile.getPath();
        try {
            return toAttachmentFileResponse(readAllBytes(Paths.get(path)),
                                            savedFile.getOriginalName());
        } catch (final IOException | ServiceIOException e) {
            final String error = RETRIEVE_ERROR.formatted(path);
            LOGGER.error(error);
            throw new ServiceIOException(error, e);
        }
    }

    /**
     * Used to prevent path injection attacks
     *
     * @param pathToGet    should be located in parent
     * @param parentFolder should contain path
     * @return the path to get as a Path object
     * @throws ServiceIOException if not the case, or if paths are invalid
     */
    public static Path getIfInside(final String pathToGet,
                                   final Path parentFolder) throws ServiceIOException {
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

/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public final class JsonUtils {

    public static <T> T read(final Class<T> clazz,
                             final String path) throws FileNotFoundException {
        return read(clazz, new FileInputStream(path));
    }

    public static <T> T read(final Class<T> clazz,
                             final InputStream inputStream) {
        try (inputStream) {
            return mapperWithIndent().readValue(inputStream, clazz);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when converting Json file to object of type %s",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> T read(final Class<T> clazz,
                             final MultipartFile file) throws IOException {
        return read(clazz, new ByteArrayInputStream(file.getBytes()));
    }

    public static <T> byte[] writeToBytes(final Class<T> clazz,
                                          final T object) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeInStream(clazz, object, outputStream);
        return outputStream.toByteArray();
    }

    public static <T> void writeInPath(final Class<T> clazz,
                                       final T object,
                                       final Path filePath) throws IOException {
        writeInStream(clazz, object, Files.newOutputStream(filePath));
    }

    public static <T> void writeInStream(final Class<T> clazz,
                                         final T object,
                                         final OutputStream outputStream) {
        try (outputStream) {
            mapperWithIndent().writeValue(outputStream, object);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    private static ObjectMapper mapperWithIndent() {
        return new ObjectMapper().enable(INDENT_OUTPUT);
    }

}


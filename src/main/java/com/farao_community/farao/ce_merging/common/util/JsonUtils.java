/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static java.nio.file.Files.newOutputStream;

public final class JsonUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(INDENT_OUTPUT);

    private JsonUtils() {
        // utility class
    }

    /**
     *
     * @param clazz the Class object representing T
     * @param <T>   the class of the object
     * @param path  the path of the file to read from
     * @return an object from a JSON file path
     * @throws FileNotFoundException if the file does not exist
     */
    public static <T> T read(final Class<T> clazz,
                             final String path) throws FileNotFoundException {
        return read(clazz, new FileInputStream(path));
    }

    /**
     *
     * @param clazz       the Class object representing T
     * @param <T>         the class of the object
     * @param inputStream the stream to read the object from
     * @return an object from a JSON stream
     */
    public static <T> T read(final Class<T> clazz,
                             final InputStream inputStream) {
        try (inputStream) {
            return MAPPER.readValue(inputStream, clazz);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when converting JSON file to object of type %s",
                                                      clazz.getSimpleName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    /**
     *
     * @param clazz the Class object representing T
     * @param <T>   the class of the object
     * @param file  the  file to read from
     * @return an object from a JSON file
     * @throws IOException if the file does not exist or is invalid
     */
    public static <T> T read(final Class<T> clazz,
                             final MultipartFile file) throws IOException {
        return read(clazz, new ByteArrayInputStream(file.getBytes()));
    }

    /**
     *
     * @param clazz  the Class object representing T
     * @param <T>    the class of the object
     * @param object the object to serialize
     * @return the object as a (JSON text) byte array
     */
    public static <T> byte[] writeToBytes(final Class<T> clazz,
                                          final T object) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeInStream(clazz, object, outputStream);
        return outputStream.toByteArray();
    }

    /**
     *
     * @param clazz    the Class object representing T
     * @param <T>      the class of the object
     * @param object   the object to serialize
     * @param filePath the path of the file to serialize object to
     * @throws IOException if there's a problem with the streams
     */
    public static <T> void writeInPath(final Class<T> clazz,
                                       final T object,
                                       final Path filePath) throws IOException {
        writeInStream(clazz, object, newOutputStream(filePath));
    }

    /**
     *
     * @param clazz        the Class object representing T
     * @param <T>          the class of the object
     * @param object       the object to serialize
     * @param outputStream the output stream to serialize object to
     */
    public static <T> void writeInStream(final Class<T> clazz,
                                         final T object,
                                         final OutputStream outputStream) {
        try (outputStream) {
            MAPPER.writeValue(outputStream, object);
        } catch (final IOException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s",
                                                      clazz.getSimpleName());
            LOGGER.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

}

